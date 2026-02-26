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

def expand_list(input_list):
    '''Main expansion logic: ["1-3", 5] -> [1, 2, 3, 5]'''

    # Ensure input is a list, even if a single string/int is passed
    if not isinstance(input_list, list):
        input_list = [input_list]

    # Use a set to automatically handle duplicates (e.g., [1-5, 3-6])
    results = set()

    for item in input_list:
        # CASE 1: Item is already an integer
        if isinstance(item, int):
            # CATCH: Negative values. We don't want to allow those.
            if item < 0:
                raise ValueError(
                    f"Received negative integer {item}."
                )
            results.add(item)

        # CASE 2: Item is a string (potentially a range like "1-10")
        elif isinstance(item, str):
            item = item.replace(" ", "") # Remove spaces for "1 - 10" support
            if '-' in item:
                try:
                    # Split only on the first dash to handle potential issues
                    parts = item.split('-')
                    if len(parts) != 2: raise ValueError

                    # Convert start/end to ints and generate the range
                    start, end = map(int, parts)
                    # Handle both "1-5" and "5-1" safely
                    low, high = (start, end) if start <= end else (end, start)
                    results.update(range(low, high + 1))
                except ValueError:
                    raise ValueError(f"Invalid range format: '{item}'")

            # String is just a number like "42"
            elif item.isdigit():
                results.add(int(item))
            else:
                raise ValueError(f"Invalid string item: '{item}'")

        # CASE 3: Item is a float or something else unsupported
        else:
            raise ValueError(f"Unsupported type {type(item)}")

    # Convert set back to a sorted list for the final output
    return sorted(list(results))

def contract_list(input_list):
    '''Main contraction logic: [1, 2, 3, 5] -> ['1-3', 5]'''
    from itertools import groupby
    from operator import itemgetter

    # 1. Clean up and validate the input by running it through expand_list
    # This ensures we have a unique, sorted list of integers to work with.
    try:
        nums = expand_list(input_list)
    except ValueError as e:
        # Re-raising the error so the user sees the specific "quote" warning
        raise ValueError(f"Error in contract_list: {e}")

    # Handle empty input gracefully
    if not nums:
        return []

    result = []

    # 2. The Grouping Recipe:
    # We enumerate the list: [(0, 10), (1, 11), (2, 12), (3, 15)]
    # We subtract index from value: (10-0=10), (11-1=10), (12-2=10), (15-3=12)
    # Consecutive numbers will always yield the same result (the 'key').
    for key, group in groupby(enumerate(nums), lambda x: x[1] - x[0]):
        # Extract just the numbers from the group
        group = list(map(itemgetter(1), group))

        if len(group) > 1:
            # If the group has more than 1 item, it's a range (start-end)
            result.append(f'{group[0]}-{group[-1]}')
        else:
            # If it's alone, just add the number
            result.append(group[0])

    return result

class FilterModule(object):
    ''' Ansible vitam jinja2 filters '''

    def filters(self):
        return {
            # jinja2 overrides
            'client_url': client_url,
            'get_certificates': get_certificates,
            'get_certificates_from_context_id': get_certificates_from_context_id,
            'get_certificates_from_context_name': get_certificates_from_context_name,
            'get_contexts': get_contexts,
            'contract_list': contract_list,
            'expand_list': expand_list
        }
