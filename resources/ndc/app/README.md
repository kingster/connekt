Installing
=================

### NM

This package depends on openssl1.0.2 which is not available in nm. Hence you will need to add in the repos for it. 
The specific dependency has been extracted and made available via a custom repo. Run the following commands on your nm machine for it 

```bash
echo "deb http://10.47.2.22:80/repos/openssl102/1 /" | sudo tee /etc/apt/sources.list.d/openssl.list
```

Optionally it takes in configuration for specter bucket in case you want to change default bucket for particular machine

```bash
echo "fk-pf-connekt fk-pf-connekt/specter-bucket text prod-fdpingestion-specter-nm" | sudo debconf-set-selections
```

### NDC

Follow the setup.sh and update.sh scripts.
