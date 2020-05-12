// package autoscaler;

// public class AutoScaler {
//     static AmazonEC2 ec2Client;

//     public static void initEc2Client() throws Exception {
//         ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
//         try {
//             credentialsProvider.getCredentials();
//         } catch (Exception e) {
//             throw new AmazonClientException(
//                     "Cannot load the credentials from the credential profiles file. " +
//                     "Please make sure that your credentials file is at the correct " +
//                     "location (~/.aws/credentials), and is in valid format.",
//                     e);
//         }
//         ec2Client = AmazonEC2ClientBuilder.standard()
//             .withCredentials(credentialsProvider)
//             .withRegion("us-east-1")
//             .build();
//     }

//     public static void main(final String[] args) throws Exception {
//         initEc2Client();
//     }
// }