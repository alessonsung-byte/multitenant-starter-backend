#!/bin/bash

# 在部署主機上建立 api-server 的日誌目錄（docker-compose-server.yml 會把容器內的日誌映射到 /opt/starter/logs/api）
sudo mkdir -p /opt/starter/logs/api

# 取得目前使用者的主要群組
CURRENT_GROUP=$(id -gn)

# 指定資料夾的擁有者為 root，群組為目前使用者的主要群組
sudo chown -R root:"$CURRENT_GROUP" /opt/starter

# 設置資料夾權限為 770
sudo chmod -R 770 /opt/starter

echo "Directory '/opt/starter' created with user root, group $CURRENT_GROUP, and permission 770"
