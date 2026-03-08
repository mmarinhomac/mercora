package com.mercora.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalStack extends Stack {
  private final Vpc vpc;
  private final Cluster ecsCluster;

  public LocalStack(final App scope, final String id, final StackProps props) {
    super(scope, id, props);

    this.vpc = createVpc();

    DatabaseInstance orderServiceDb =
            createDatabase("OrderServiceDB", "order-service-db");

    DatabaseInstance paymentServiceDb =
            createDatabase("PaymentServiceDB", "payment-service-db");

    DatabaseInstance authServiceDb =
            createDatabase("AuthServiceDB", "auth-service-db");

    CfnHealthCheck orderDbHealthCheck =
            createDbHealthCheck(orderServiceDb, "OrderServiceDBHealthCheck");

    CfnHealthCheck paymentDbHealthCheck =
            createDbHealthCheck(paymentServiceDb, "PaymentServiceDBHealthCheck");

    CfnHealthCheck authDbHealthCheck =
            createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

    CfnCluster mskCluster = createMskCluster();

    this.ecsCluster = createEcsCluster();

    FargateService authService =
            createFargateService("AuthService",
                    "auth-service",
                    List.of(4004),
                    authServiceDb,
                    Map.of("JWT_SECRET", "8hf9KAJEeATIvtf5E2wzeZsoM9zaMr3O1aQTbCPngV6"));

    authService.getNode().addDependency(authDbHealthCheck);
    authService.getNode().addDependency(authServiceDb);

    FargateService inventoryService =
            createFargateService("InventoryService",
                    "inventory-service",
                    List.of(4003),
                    null,
                    null);
    inventoryService.getNode().addDependency(mskCluster);

    FargateService paymentService =
            createFargateService("PaymentService",
                    "payment-service",
                    List.of(4002, 9001),
                    paymentServiceDb,
                    null);
    paymentService.getNode().addDependency(paymentDbHealthCheck);
    paymentService.getNode().addDependency(paymentServiceDb);

    FargateService orderService = createFargateService("OrderService",
            "order-service",
            List.of(4001),
            orderServiceDb,
            Map.of(
                    "PAYMENT_SERVICE_ADDRESS", "host.docker.internal",
                    "PAYMENT_SERVICE_GRPC_PORT", "9001"
            ));
    orderService.getNode().addDependency(orderDbHealthCheck);
    orderService.getNode().addDependency(orderServiceDb);
    orderService.getNode().addDependency(paymentService);
    orderService.getNode().addDependency(mskCluster);

    createApiGatewayService();
  }

  private Vpc createVpc() {
    return Vpc.Builder
            .create(this, "MercoraVPC")
            .vpcName("MercoraVPC")
            .maxAzs(2)
            .build();
  }

  private DatabaseInstance createDatabase(String id, String dbName) {
    return DatabaseInstance.Builder
            .create(this, id)
            .engine(DatabaseInstanceEngine.postgres(
                    PostgresInstanceEngineProps.builder()
                            .version(PostgresEngineVersion.VER_17_2)
                            .build()))
            .vpc(vpc)
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
            .allocatedStorage(20)
            .credentials(Credentials.fromGeneratedSecret("admin"))
            .databaseName(dbName)
            .removalPolicy(RemovalPolicy.DESTROY)
            .build();
  }

  private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
    return CfnHealthCheck.Builder.create(this, id)
            .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                    .type("TCP")
                    .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                    .ipAddress(db.getDbInstanceEndpointAddress())
                    .requestInterval(30)
                    .failureThreshold(3)
                    .build())
            .build();
  }

  private CfnCluster createMskCluster() {
    return CfnCluster.Builder.create(this, "MskCluster")
            .clusterName("kafka-cluster")
            .kafkaVersion("3.6.0")
            .numberOfBrokerNodes(2)
            .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                    .instanceType("kafka.m5.xlarge")
                    .clientSubnets(vpc.getPrivateSubnets().stream()
                            .map(subnet -> subnet.getSubnetId())
                            .toList())
                    .brokerAzDistribution("DEFAULT")
                    .build())
            .build();
  }

  private Cluster createEcsCluster() {
    return Cluster.Builder.create(this, "MercoraCluster")
            .vpc(vpc)
            .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                    .name("mercora.local")
                    .build())
            .build();
  }

  private FargateService createFargateService(String id,
                                              String imageName,
                                              List<Integer> ports,
                                              DatabaseInstance db,
                                              Map<String, String> additionalEnvVars) {

    FargateTaskDefinition taskDefinition =
            FargateTaskDefinition.Builder.create(this, id + "Task")
                    .cpu(256)
                    .memoryLimitMiB(512)
                    .build();

    ContainerDefinitionOptions.Builder containerOptions =
            ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromRegistry(imageName))
                    .portMappings(ports.stream()
                            .map(port -> PortMapping.builder()
                                    .containerPort(port)
                                    .hostPort(port)
                                    .protocol(Protocol.TCP)
                                    .build())
                            .toList())
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                    .logGroupName("/ecs/" + imageName)
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .retention(RetentionDays.ONE_DAY)
                                    .build())
                            .streamPrefix(imageName)
                            .build()));

    Map<String, String> envVars = new HashMap<>();
    envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

    if (additionalEnvVars != null) {
      envVars.putAll(additionalEnvVars);
    }

    if (db != null) {
      envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
              db.getDbInstanceEndpointAddress(),
              db.getDbInstanceEndpointPort(),
              imageName
      ));
      envVars.put("SPRING_DATASOURCE_USERNAME", "admin");
      envVars.put("SPRING_DATASOURCE_PASSWORD",
              db.getSecret().secretValueFromJson("password").unsafeUnwrap());
      envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
      envVars.put("SPRING_SQL_INIT_MODE", "always");
      envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
    }

    containerOptions.environment(envVars);
    taskDefinition.addContainer(imageName + "Container", containerOptions.build());

    return FargateService.Builder.create(this, id)
            .cluster(ecsCluster)
            .taskDefinition(taskDefinition)
            .assignPublicIp(false)
            .serviceName(imageName)
            .build();
  }

  private void createApiGatewayService() {
    FargateTaskDefinition taskDefinition =
            FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                    .cpu(256)
                    .memoryLimitMiB(512)
                    .build();

    ContainerDefinitionOptions containerOptions =
            ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromRegistry("gateway"))
                    .environment(Map.of(
                            "SPRING_PROFILES_ACTIVE", "prod",
                            "AUTH_SERVICE_URL", "http://auth-service.mercora.local:4004"
                    ))
                    .portMappings(List.of(4000).stream()
                            .map(port -> PortMapping.builder()
                                    .containerPort(port)
                                    .hostPort(port)
                                    .protocol(Protocol.TCP)
                                    .build())
                            .toList())
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                    .logGroupName("/ecs/api-gateway")
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .retention(RetentionDays.ONE_DAY)
                                    .build())
                            .streamPrefix("api-gateway")
                            .build()))
                    .build();


    taskDefinition.addContainer("APIGatewayContainer", containerOptions);

    ApplicationLoadBalancedFargateService apiGateway =
            ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                    .cluster(ecsCluster)
                    .serviceName("gateway")
                    .taskDefinition(taskDefinition)
                    .desiredCount(1)
                    .healthCheckGracePeriod(Duration.seconds(60))
                    .build();
  }

  public static void main(final String[] args) {
    App app = new App(AppProps.builder().outdir("./cdk.out").build());

    StackProps props = StackProps.builder()
            .synthesizer(new BootstraplessSynthesizer())
            .build();

    new LocalStack(app, "localstack", props);
    app.synth();
    System.out.println("App synthesizing in progress...");
  }
}
