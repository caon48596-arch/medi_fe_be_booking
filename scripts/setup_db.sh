#!/bin/bash
sudo service redis-server start

# Configure postgresql
sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" /etc/postgresql/*/main/postgresql.conf
sudo sed -i "s/listen_addresses = 'localhost'/listen_addresses = '*'/g" /etc/postgresql/*/main/postgresql.conf
sudo sed -i 's/scram-sha-256/trust/g' /etc/postgresql/*/main/pg_hba.conf
sudo sed -i 's/peer/trust/g' /etc/postgresql/*/main/pg_hba.conf
sudo sed -i 's/md5/trust/g' /etc/postgresql/*/main/pg_hba.conf
echo "host all all 0.0.0.0/0 trust" | sudo tee -a /etc/postgresql/*/main/pg_hba.conf
echo "host all all 127.0.0.1/32 trust" | sudo tee -a /etc/postgresql/*/main/pg_hba.conf

sudo service postgresql restart
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres createdb emedical_db 2>/dev/null || true
echo "POSTGRES_READY"
