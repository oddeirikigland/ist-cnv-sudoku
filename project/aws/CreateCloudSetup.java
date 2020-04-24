import java.util.Arrays;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;

public class CreateCloudSetup {
    private static final String loadbalancerName = "my-load-balancer";
    private static final String targetgroup = "my-target-group";
    private static final String launchConfigurationName = "my-lc-from-instance-changetype";
    private static final String autoScalingGroupName = "my-asg-from-instance";
    private static final String policyIncreaseName = "my-simple-scale-in-policy-increase";
    private static final String policyDecreaseName = "my-simple-scale-in-policy-decrease";
    private static final String alarmHighName = "Step-Scaling-AlarmHigh-AddCapacity";
    private static final String alarmLowName = "Step-Scaling-AlarmLow-RemoveCapacity";
    private static final String security_group = "sg-04ceefaf6f626fba3";
    private static final String image_id = "ami-0966bb7855730320b";
    private static final String key_name = "cloud-compute-lab3";
    private static final String security_group_name = "launch-wizard-1-cloud-compute";

    /*
     * Before running the code:
     *      Fill in your AWS access credentials in the provided credentials
     *      file template, and be sure to move the file to the default location
     *      (~/.aws/credentials) where the sample code will load the
     *      credentials from.
     *      https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING:
     *      To avoid accidental leakage of your credentials, DO NOT keep
     *      the credentials file in your source directory.
     */
    static AmazonElasticLoadBalancing elb;

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private static void init() throws Exception {

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        elb = AmazonElasticLoadBalancingClientBuilder.standard().withRegion("us-east-1").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
    }


    public static void main(String[] args) throws Exception {
        System.out.println("===========================================");
        System.out.println("Welcome to the AWS Java SDK!");
        System.out.println("===========================================");

        init();

        try {
            // create load balancer
            CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
            lbRequest.setType("classic");
            lbRequest.setName(loadbalancerName);
            lbRequest.setSubnets(Arrays.asList("subnet-00866866", "subnet-3b37dc64"));
            lbRequest.setSecurityGroups(Arrays.asList(security_group));
            // List<Listener> listeners = new ArrayList<Listener>(1);
            // listeners.add(new Listener("HTTP", 80, 80));
            // Listener listener = new Listener();
            // listener.setProtocol(ProtocolEnum.HTTP);
            // listener.setPort(80);
            // listener.setDefaultActions(Collection<Action> defaultActions);
            
            // lbRequest.setListeners(Arrays.asList(new Listener("HTTP", 80, 8000)));

            CreateLoadBalancerResult lbResult = elb.createLoadBalancer(lbRequest);
            System.out.println("created load balancer loader");

            


        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }
    }
}
