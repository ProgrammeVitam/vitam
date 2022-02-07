Restic
======

Restic is a fast and secure backup program. It is used by Vitam to backup databases (such as mongodb) on storage-offers.

Requirements
------------

The `restic` package is available under vitam-extra repository.

The `mongodb-org-tools` package is mandatory to execute mongodump commands.

Role Variables
--------------

* Variables can be overloaded under `environments/group_vars/all/cots_vars.yml`, the default values are:

  ```yml
  restic:
    snapshot_retention: 30 # number of snapshot to keep
    cron:
      minute: '00'
      hour: '23'
      day: '*'
      month: '*'
      weekday: '*'
  ```

  > The cron will be installed with vitam user. To manually edit, use `crontab -e -u vitam`.

* Custom backups can be configured under `environments/group_vars/all/cots_vars.yml`:

  Example for a global mongodb backup:

  ```yml
  restic:
    backup:
      # mongo-offer
      - name: "{{ offer_conf }}"
        type: mongodb
        host: "{{ offer_conf }}-mongos.service.consul:{{ mongodb.mongos_port }}"
        user: "{{ mongodb[offer_conf].admin.user }}"
        password: "{{ mongodb[offer_conf].admin.password }}"
      # mongo-data
      - name: mongo-data
        type: mongodb
        host: "mongo-data-mongos.service.consul:{{ mongodb.mongos_port }}"
        user: "{{ mongodb['mongo-data'].admin.user }}"
        password: "{{ mongodb['mongo-data'].admin.password }}"
      # mongo-vitamui
      - name: mongo-vitamui
        type: mongodb
        host: "mongo-vitamui-mongod.service.consul:{{ mongodb.mongod_port }}"
        # Add the following params on environments/group_vars/all/vault-vitam.yml
        # They can be found on vitamui deployment sources under environments/group_vars/all/vault-mongodb.yml
        user: "{{ mongodb['mongo-vitamui'].admin.user }}"
        password: "{{ mongodb['mongo-vitamui'].admin.password }}"
  ```

* Default password for restic backups is `vitam`. You can update it under `environments/group_vars/all/vault-cots.yml`.

  ```yml
  restic:
    password: 'my_custom_password'
  ```

  > WARNING: Be careful if you edit the default password.
  > Remembering your password is important! If you lose it, you won’t be able to access data stored in the repository.

Example Playbook
----------------

```yml
- hosts: hosts_storage_offer_default
  any_errors_fatal: yes
  gather_facts: no
  roles:
    - { role: restic, when: "restic_enabled | default(false) | bool == true" }
```

License
-------

CeCILL

Author Information
------------------

Programme Vitam <support@programmevitam.fr>
