# aws config

## Download aws cli

```bash
wget -P ~/ http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip
unzip ~/aws-java-sdk-1.11.762.zip -d ~/
```

Create `.aws` folder containing a file called credentials. Get them from AWS Console.

```bash
mkdir ~/.aws
echo "[default]\n\naws_access_key_id=<your-aws-access-key-id>\n\naws_secret_access_key=<your-aws-secret-access-key>" > ~/.aws/credentials
```

## Setup

Sets config for whole project including aws sdk

```bash
source setup.sh configure aws-cli
```
