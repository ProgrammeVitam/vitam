# (C) Vitam toussa toussa...

__metaclass__ = type

def client_url(vitam_struct):
    '''Compute the client url based on a server url description'''
    if vitam_struct["https_enabled"]:
        if 'port_https' in vitam_struct:
            return "https://%s:%s/" % (vitam_struct["host"], vitam_struct["port_https"])
        else:
            return "https://%s:%s/" % (vitam_struct["host"], vitam_struct["port_service"])
    else:
        if 'port_http' in vitam_struct:
            return "http://%s:%s/" % (vitam_struct["host"], vitam_struct["port_http"])
        else:
            return "http://%s:%s/" % (vitam_struct["host"], vitam_struct["port_service"])

def remove_skipped_servers(result):
    '''Remove the skipped hosts'''
    new_list = []
    for elem in result:
        if not 'skipped' in elem:
            new_list.append(elem)
        if 'skipped' in elem and elem['skipped'] != True:
            new_list.append(elem)
    return new_list

def get_certificates(securityprofiles_struct, securityprofile_identifier):
    '''Get present certificates list from a securityprofiles structure'''
    certificates_list = []
    for secuprof in securityprofiles_struct:
        if secuprof['identifier'] == securityprofile_identifier \
        and 'contexts' in secuprof.keys():
            for context in secuprof['contexts']:
                if 'certificates' in context.keys():
                    certificates_list += context['certificates']
    return certificates_list

def get_certificates_from_context_id(securityprofiles_struct, securityprofile_identifier, context_id):
    '''Get present certificates list from a securityprofiles structure'''
    certificates_list = []
    for secuprof in securityprofiles_struct:
        if secuprof['identifier'] == securityprofile_identifier \
        and 'contexts' in secuprof.keys():
            for context in secuprof['contexts']:
                if 'certificates' in context.keys() and context_id == context['identifier']:
                    certificates_list += context['certificates']
    return certificates_list

def get_certificates_from_context_name(securityprofiles_struct, securityprofile_identifier, context_name):
    '''Get present certificates list from a securityprofiles structure'''
    certificates_list = []
    for secuprof in securityprofiles_struct:
        if secuprof['identifier'] == securityprofile_identifier \
        and 'contexts' in secuprof.keys():
                for context in secuprof['contexts']:
                    if 'certificates' in context.keys() and context_name == context['name']:
                        certificates_list += context['certificates']
    return certificates_list

def get_contexts(securityprofiles_struct, securityprofile_identifier):
    '''Get present certificates list from a securityprofiles structure'''
    contexts_list = []
    for secuprof in securityprofiles_struct:
        if secuprof['identifier'] == securityprofile_identifier \
        and 'contexts' in secuprof.keys():
                for context in secuprof['contexts']:
                    contexts_list.append(context['name'])
    return contexts_list

class FilterModule(object):
    ''' Ansible vitam jinja2 filters '''

    def filters(self):
        return {
            # jinja2 overrides
            'client_url': client_url,
            'remove_skipped_servers': remove_skipped_servers,
            'get_certificates': get_certificates,
            'get_certificates_from_context_id': get_certificates_from_context_id,
            'get_certificates_from_context_name': get_certificates_from_context_name,
            'get_contexts': get_contexts
        }
