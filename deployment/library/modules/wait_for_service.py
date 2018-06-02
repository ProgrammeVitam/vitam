#!/usr/bin/python
# -*- coding: utf-8 -*-

from __future__ import absolute_import, division, print_function
__metaclass__ = type

import time
import datetime
from ansible.module_utils.basic import AnsibleModule
from ansible.module_utils.service import sysv_exists, sysv_is_enabled, fail_if_missing
from ansible.module_utils._text import to_native


def is_running_service(service_status):
    return service_status['ActiveState'] in set(['active', 'activating'])


def request_was_ignored(out):
    return '=' not in out and 'ignoring request' in out


def parse_systemctl_show(lines):
    parsed = {}
    multival = []
    k = None
    for line in lines:
        if k is None:
            if '=' in line:
                k, v = line.split('=', 1)
                if k.startswith('Exec') and v.lstrip().startswith('{'):
                    if not v.rstrip().endswith('}'):
                        multival.append(v)
                        continue
                parsed[k] = v.strip()
                k = None
        else:
            multival.append(line)
            if line.rstrip().endswith('}'):
                parsed[k] = '\n'.join(multival).strip()
                multival = []
                k = None
    return parsed


def call_systemd(module, unit):
    # Vars
    rc=0
    found=False
    result = dict(
        name=unit,
        changed=False,
        status=dict()
    )
    is_initd = sysv_exists(unit)
    is_systemd = False
    # Launch systemctl command
    systemctl = module.get_bin_path('systemctl', True)
    (rc, out, err) = module.run_command("%s show '%s'" % (systemctl, unit))
    if request_was_ignored(out) or request_was_ignored(err):
        # fallback list-unit-files as show does not work on some systems (chroot)
        # not used as primary as it skips some services (like those using init.d) and requires .service/etc notation
        (rc, out, err) = module.run_command("%s list-unit-files '%s'" % (systemctl, unit))
        if rc == 0:
            is_systemd = True
    elif rc == 0:
        # load return of systemctl show into dictionary for easy access and return
        if out:
            result['status'] = parse_systemctl_show(to_native(out).split('\n'))
            is_systemd = 'LoadState' in result['status'] and result['status']['LoadState'] != 'not-found'
            # Check for loading error
            if is_systemd and 'LoadError' in result['status']:
                module.fail_json(msg="Error loading unit file '%s': %s" % (unit, result['status']['LoadError']))
    else:
        # Check for systemctl command
        module.run_command(systemctl, check_rc=True)
    # Does service exist?
    found = is_systemd or is_initd
    if is_initd and not is_systemd:
        module.warn('The service (%s) is actually an init script but the system is managed by systemd' % unit)
    return found, result



def main():
    # initialize
    module = AnsibleModule(
        argument_spec=dict(
            name=dict(type='str', aliases=['service', 'unit']),
            state=dict(type='str', choices=['started', 'stopped']),
            timeout=dict(default=300, type='int')
        ),
        supports_check_mode=True,
    )
    start = datetime.datetime.utcnow()
    unit = module.params['name']
    if unit:
        end = start + datetime.timedelta(seconds=module.params['timeout'])
        service_state_ok = False
        # Loop
        while datetime.datetime.utcnow() < end:
            (found,result) = call_systemd(module, unit)
            # Check service state
            if module.params['state'] is not None:
                fail_if_missing(module, found, unit, msg="host")
                # What is current service state?
                if 'ActiveState' in result['status']:
                    if module.params['state'] == 'stopped':
                        # If service is stopped -> break
                        if not is_running_service(result['status']):
                            service_state_ok  = True
                            break
                    elif module.params['state'] == 'started':
                        # If service is started -> break
                        if is_running_service(result['status']):
                            service_state_ok  = True
                            break
            time.sleep(1)
        if service_state_ok == False:
            elapsed = datetime.datetime.now() - start
            module.fail_json(msg="Timeout when waiting for %s service status to be %s" % (unit, module.params['state']), elapsed=elapsed.seconds)
    module.exit_json(**result)


if __name__ == '__main__':
    main()
