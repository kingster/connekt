Connekt Deployment
=======================

1. To Deploy Receptors's update the inventory with the updated host's.

Receptors
---------
- Deployment

  ```bash
  ansible-playbook -i inventory.txt playbooks/deploy-receptors.yaml
  ```
- Restart

  ```bash
  ansible-playbook -i inventory.txt playbooks/restart-receptors.yaml
  ```
  
Busybees
---------
- Deployment

  ```bash
  ansible-playbook -i inventory.txt playbooks/deploy-busybees.yaml`
  ```

