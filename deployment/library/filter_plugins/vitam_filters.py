# (C) Vitam toussa toussa...

__metaclass__ = type


def client_url(vitam_struct):
    '''Compute the client url based on a server url description'''
    if vitam_struct["https_enabled"]:
        return "https://%s:%s/" % (vitam_struct["host"], vitam_struct["port_https"])
    else:
        return "http://%s:%s/" % (vitam_struct["host"], vitam_struct["port"])


class FilterModule(object):
    ''' Ansible vitam jinja2 filters '''

    def filters(self):
        return {
            # jinja2 overrides
            'client_url': client_url
        }
