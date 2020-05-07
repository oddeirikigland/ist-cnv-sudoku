# aws config

## Download aws cli

```bash
wget -P ~/ http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip
unzip ~/aws-java-sdk-1.11.762.zip -d ~/
```

## Setup

Sets java to version 7 and adds aws cli to classpath

```bash
export CLASSPATH=$CLASSPATH:~/aws-java-sdk-1.11.762/lib/aws-java-sdk-1.11.762.jar:~/aws-java-sdk-1.11.762/third-party/lib/*:.
```

Create `.aws` folder containing a file called credentials. Get them from AWS Console.

```bash
mkdir ~/.aws
echo "[default]\n\naws_access_key_id=<your-aws-access-key-id>\n\naws_secret_access_key=<your-aws-secret-access-key>" > ~/.aws/credentials
```

## Run

Run this script to create load balancer with auto scaler for a given aws EC2 instance

```bash
chmod +x aws_setup.sh
./aws_setup.sh
```