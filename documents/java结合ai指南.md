# Java与AI结合

## 📋 目录概览

本指南专注于Java在人工智能领域的应用，涵盖从基础概念到企业级应用的完整体系。

---

## 🎯 第一部分：Java AI生态系统概述

### 1.1 Java在AI领域的优势

#### 核心优势
- **跨平台兼容性**：一次编写，到处运行
- **企业级稳定性**：成熟的JVM生态系统
- **丰富的库支持**：庞大的开源社区
- **高性能计算**：JVM优化和并发处理能力
- **企业集成**：与现有Java系统无缝集成

#### 应用场景
- **大数据处理**：Spark、Hadoop生态
- **企业级AI应用**：业务系统集成
- **实时推理服务**：微服务架构
- **分布式机器学习**：集群计算
- **AI平台开发**：完整的AI解决方案

### 1.2 Java AI技术栈

```
┌─────────────────────────────────────────┐
│              应用层                      │
├─────────────────────────────────────────┤
│  Spring Boot AI | 微服务 | Web应用      │
├─────────────────────────────────────────┤
│             框架层                       │
├─────────────────────────────────────────┤
│ DL4J | Weka | MOA | Smile | Mahout      │
├─────────────────────────────────────────┤
│            计算层                        │
├─────────────────────────────────────────┤
│   Spark | Flink | Kafka | Elasticsearch │
├─────────────────────────────────────────┤
│            基础层                        │
├─────────────────────────────────────────┤
│        JVM | OpenJDK | GraalVM          │
└─────────────────────────────────────────┘
```

---

## 🛠️ 第二部分：核心AI库与框架

### 2.1 Deep Learning for Java (DL4J)

#### 简介与特点
DL4J是最成熟的Java深度学习框架，支持分布式深度学习。

#### 环境配置
```xml
<!-- Maven依赖 -->
<dependencies>
    <dependency>
        <groupId>org.deeplearning4j</groupId>
        <artifactId>deeplearning4j-core</artifactId>
        <version>1.0.0-M2</version>
    </dependency>
    <dependency>
        <groupId>org.nd4j</groupId>
        <artifactId>nd4j-native-platform</artifactId>
        <version>1.0.0-M2</version>
    </dependency>
    <dependency>
        <groupId>org.datavec</groupId>
        <artifactId>datavec-api</artifactId>
        <version>1.0.0-M2</version>
    </dependency>
</dependencies>
```

#### 基础神经网络实现
```java
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class SimpleNeuralNetwork {
    
    public static void main(String[] args) {
        // 网络配置
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(new Adam(0.001))
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(new DenseLayer.Builder()
                .nIn(784)  // 输入特征数
                .nOut(128) // 隐藏层神经元数
                .activation(Activation.RELU)
                .build())
            .layer(new DenseLayer.Builder()
                .nIn(128)
                .nOut(64)
                .activation(Activation.RELU)
                .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nIn(64)
                .nOut(10)  // 输出类别数
                .activation(Activation.SOFTMAX)
                .build())
            .build();
        
        // 创建网络
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        
        // 训练网络
        // model.fit(trainingData);
        
        System.out.println("神经网络创建成功！");
        System.out.println("网络参数数量: " + model.numParams());
    }
}
```

#### 卷积神经网络示例
```java
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;

public class CNNExample {
    
    public static MultiLayerConfiguration createCNNConfig() {
        return new NeuralNetConfiguration.Builder()
            .seed(123)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(new Adam(0.001))
            .list()
            .layer(new ConvolutionLayer.Builder(5, 5)
                .nIn(1)     // 输入通道数
                .nOut(32)   // 输出特征图数
                .stride(1, 1)
                .activation(Activation.RELU)
                .build())
            .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2, 2)
                .stride(2, 2)
                .build())
            .layer(new ConvolutionLayer.Builder(5, 5)
                .nOut(64)
                .stride(1, 1)
                .activation(Activation.RELU)
                .build())
            .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .kernelSize(2, 2)
                .stride(2, 2)
                .build())
            .layer(new DenseLayer.Builder()
                .nOut(128)
                .activation(Activation.RELU)
                .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(10)
                .activation(Activation.SOFTMAX)
                .build())
            .setInputType(InputType.convolutionalFlat(28, 28, 1))
            .build();
    }
}
```

### 2.2 Weka机器学习库

#### 库介绍
Weka是Java生态中最经典的机器学习库，提供完整的数据挖掘工具集。

#### 基础使用示例
```java
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.classifiers.trees.J48;
import weka.classifiers.Evaluation;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class WekaExample {
    
    public static void main(String[] args) throws Exception {
        // 加载数据
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File("data/iris.csv"));
        Instances data = loader.getDataSet();
        
        // 设置类属性
        data.setClassIndex(data.numAttributes() - 1);
        
        // 特征选择
        Remove remove = new Remove();
        remove.setAttributeIndices("1"); // 移除第一个属性
        remove.setInputFormat(data);
        Instances filteredData = Filter.useFilter(data, remove);
        
        // 构建分类器
        J48 tree = new J48();
        tree.buildClassifier(filteredData);
        
        // 交叉验证评估
        Evaluation eval = new Evaluation(filteredData);
        eval.crossValidateModel(tree, filteredData, 10, new Random(1));
        
        // 输出结果
        System.out.println("=== 决策树模型 ===");
        System.out.println(tree);
        System.out.println("\n=== 评估结果 ===");
        System.out.println(eval.toSummaryString());
        System.out.println("准确率: " + eval.pctCorrect() + "%");
    }
}
```

#### 聚类分析示例
```java
import weka.clusterers.SimpleKMeans;
import weka.core.EuclideanDistance;

public class ClusteringExample {
    
    public static void performKMeans(Instances data, int numClusters) throws Exception {
        // 创建K-means聚类器
        SimpleKMeans kmeans = new SimpleKMeans();
        kmeans.setNumClusters(numClusters);
        kmeans.setDistanceFunction(new EuclideanDistance());
        kmeans.setMaxIterations(100);
        
        // 执行聚类
        kmeans.buildClusterer(data);
        
        // 获取聚类中心
        Instances centroids = kmeans.getClusterCentroids();
        System.out.println("聚类中心:");
        for (int i = 0; i < centroids.numInstances(); i++) {
            System.out.println("簇 " + i + ": " + centroids.instance(i));
        }
        
        // 预测每个实例的簇
        for (int i = 0; i < data.numInstances(); i++) {
            int cluster = kmeans.clusterInstance(data.instance(i));
            System.out.println("实例 " + i + " 属于簇 " + cluster);
        }
    }
}
```

### 2.3 Smile统计机器学习库

#### 库特点
- 现代化的API设计
- 高性能算法实现
- 丰富的统计功能
- 良好的文档支持

#### 基础分类示例
```java
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.io.Read;
import smile.validation.CrossValidation;

public class SmileExample {
    
    public static void main(String[] args) throws Exception {
        // 读取数据
        DataFrame data = Read.csv("data/iris.csv");
        
        // 定义公式
        Formula formula = Formula.lhs("species");
        
        // 随机森林分类
        RandomForest model = RandomForest.fit(formula, data);
        
        // 交叉验证
        double[] accuracy = CrossValidation.classification(10, formula, data, 
            (f, x) -> RandomForest.fit(f, x));
        
        System.out.println("随机森林准确率: " + Arrays.stream(accuracy).average().orElse(0.0));
        
        // 特征重要性
        double[] importance = model.importance();
        System.out.println("特征重要性: " + Arrays.toString(importance));
    }
}
```

### 2.4 Apache Spark MLlib

#### 分布式机器学习
```java
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator;
import org.apache.spark.ml.feature.VectorAssembler;

public class SparkMLExample {
    
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
            .appName("Spark ML Example")
            .master("local[*]")
            .getOrCreate();
        
        // 加载数据
        Dataset<Row> data = spark.read()
            .option("header", "true")
            .option("inferSchema", "true")
            .csv("data/dataset.csv");
        
        // 特征工程
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(new String[]{"feature1", "feature2", "feature3"})
            .setOutputCol("features");
        
        Dataset<Row> assembledData = assembler.transform(data);
        
        // 划分训练集和测试集
        Dataset<Row>[] splits = assembledData.randomSplit(new double[]{0.8, 0.2}, 42);
        Dataset<Row> trainData = splits[0];
        Dataset<Row> testData = splits[1];
        
        // 创建逻辑回归模型
        LogisticRegression lr = new LogisticRegression()
            .setLabelCol("label")
            .setFeaturesCol("features")
            .setMaxIter(100)
            .setRegParam(0.01);
        
        // 训练模型
        LogisticRegressionModel model = lr.fit(trainData);
        
        // 预测
        Dataset<Row> predictions = model.transform(testData);
        
        // 评估
        BinaryClassificationEvaluator evaluator = new BinaryClassificationEvaluator()
            .setLabelCol("label")
            .setRawPredictionCol("rawPrediction")
            .setMetricName("areaUnderROC");
        
        double auc = evaluator.evaluate(predictions);
        System.out.println("AUC: " + auc);
        
        spark.stop();
    }
}
```

---

## 🔧 第三部分：数据处理与特征工程

### 3.1 数据预处理框架

#### 自定义数据处理工具
```java
import java.util.List;
import java.util.stream.Collectors;
import java.util.DoubleSummaryStatistics;

public class DataPreprocessor {
    
    // 数据标准化
    public static List<Double> standardize(List<Double> data) {
        DoubleSummaryStatistics stats = data.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();
        
        double mean = stats.getAverage();
        double std = Math.sqrt(data.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .average().orElse(0.0));
        
        return data.stream()
            .map(x -> (x - mean) / std)
            .collect(Collectors.toList());
    }
    
    // 数据归一化
    public static List<Double> normalize(List<Double> data) {
        DoubleSummaryStatistics stats = data.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();
        
        double min = stats.getMin();
        double max = stats.getMax();
        
        return data.stream()
            .map(x -> (x - min) / (max - min))
            .collect(Collectors.toList());
    }
    
    // 缺失值处理
    public static List<Double> handleMissingValues(List<Double> data, String method) {
        switch (method.toLowerCase()) {
            case "mean":
                double mean = data.stream()
                    .filter(x -> x != null && !x.isNaN())
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);
                return data.stream()
                    .map(x -> (x == null || x.isNaN()) ? mean : x)
                    .collect(Collectors.toList());
            
            case "median":
                List<Double> sorted = data.stream()
                    .filter(x -> x != null && !x.isNaN())
                    .sorted()
                    .collect(Collectors.toList());
                double median = sorted.size() % 2 == 0 ? 
                    (sorted.get(sorted.size()/2 - 1) + sorted.get(sorted.size()/2)) / 2.0 :
                    sorted.get(sorted.size()/2);
                return data.stream()
                    .map(x -> (x == null || x.isNaN()) ? median : x)
                    .collect(Collectors.toList());
            
            default:
                return data;
        }
    }
}
```

### 3.2 特征工程工具

#### 特征选择器
```java
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class FeatureSelector {
    
    // 相关性特征选择
    public static List<String> selectByCorrelation(Map<String, List<Double>> features, 
                                                  List<Double> target, 
                                                  double threshold) {
        List<String> selectedFeatures = new ArrayList<>();
        
        for (Map.Entry<String, List<Double>> entry : features.entrySet()) {
            double correlation = calculateCorrelation(entry.getValue(), target);
            if (Math.abs(correlation) > threshold) {
                selectedFeatures.add(entry.getKey());
            }
        }
        
        return selectedFeatures;
    }
    
    // 皮尔逊相关系数计算
    private static double calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size()) {
            throw new IllegalArgumentException("向量长度不匹配");
        }
        
        double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double numerator = 0.0;
        double sumSquaredX = 0.0;
        double sumSquaredY = 0.0;
        
        for (int i = 0; i < x.size(); i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;
            
            numerator += diffX * diffY;
            sumSquaredX += diffX * diffX;
            sumSquaredY += diffY * diffY;
        }
        
        double denominator = Math.sqrt(sumSquaredX * sumSquaredY);
        return denominator == 0 ? 0 : numerator / denominator;
    }
    
    // 方差特征选择
    public static List<String> selectByVariance(Map<String, List<Double>> features, 
                                               double threshold) {
        List<String> selectedFeatures = new ArrayList<>();
        
        for (Map.Entry<String, List<Double>> entry : features.entrySet()) {
            double variance = calculateVariance(entry.getValue());
            if (variance > threshold) {
                selectedFeatures.add(entry.getKey());
            }
        }
        
        return selectedFeatures;
    }
    
    private static double calculateVariance(List<Double> data) {
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return data.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .average().orElse(0.0);
    }
}
```

---

## 🚀 第四部分：企业级AI应用开发

### 4.1 Spring Boot AI集成

#### 项目结构
```
src/
├── main/
│   ├── java/
│   │   └── com/example/ai/
│   │       ├── AiApplication.java
│   │       ├── controller/
│   │       │   └── PredictionController.java
│   │       ├── service/
│   │       │   └── ModelService.java
│   │       ├── model/
│   │       │   └── PredictionRequest.java
│   │       └── config/
│   │           └── ModelConfig.java
│   └── resources/
│       ├── application.yml
│       └── models/
│           └── trained_model.json
```

#### Spring Boot主应用
```java
@SpringBootApplication
@EnableScheduling
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
```

#### 模型服务类
```java
@Service
@Slf4j
public class ModelService {
    
    private MultiLayerNetwork model;
    
    @PostConstruct
    public void initializeModel() {
        try {
            // 加载预训练模型
            File modelFile = new File("models/trained_model.json");
            if (modelFile.exists()) {
                model = MultiLayerNetwork.load(modelFile, false);
                log.info("模型加载成功");
            } else {
                log.warn("模型文件不存在，使用默认配置");
                initializeDefaultModel();
            }
        } catch (Exception e) {
            log.error("模型初始化失败", e);
            throw new RuntimeException("模型初始化失败", e);
        }
    }
    
    private void initializeDefaultModel() {
        // 创建默认模型配置
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(new Adam(0.001))
            .list()
            .layer(new DenseLayer.Builder()
                .nIn(10)
                .nOut(50)
                .activation(Activation.RELU)
                .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(50)
                .nOut(1)
                .activation(Activation.IDENTITY)
                .build())
            .build();
        
        model = new MultiLayerNetwork(conf);
        model.init();
    }
    
    public double predict(double[] features) {
        if (model == null) {
            throw new IllegalStateException("模型未初始化");
        }
        
        try {
            INDArray input = Nd4j.create(features).reshape(1, features.length);
            INDArray output = model.output(input);
            return output.getDouble(0);
        } catch (Exception e) {
            log.error("预测失败", e);
            throw new RuntimeException("预测失败", e);
        }
    }
    
    public List<Double> batchPredict(List<double[]> featuresList) {
        return featuresList.stream()
            .map(this::predict)
            .collect(Collectors.toList());
    }
    
    @Scheduled(fixedDelay = 3600000) // 每小时检查一次
    public void checkModelHealth() {
        if (model != null) {
            log.info("模型健康检查通过，参数数量: {}", model.numParams());
        } else {
            log.warn("模型未初始化，尝试重新加载");
            initializeModel();
        }
    }
}
```

#### REST控制器
```java
@RestController
@RequestMapping("/api/v1/predictions")
@Slf4j
public class PredictionController {
    
    @Autowired
    private ModelService modelService;
    
    @PostMapping("/single")
    public ResponseEntity<PredictionResponse> predictSingle(@RequestBody PredictionRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 输入验证
            if (request.getFeatures() == null || request.getFeatures().length == 0) {
                return ResponseEntity.badRequest()
                    .body(new PredictionResponse(null, "特征数据不能为空", null));
            }
            
            // 执行预测
            double prediction = modelService.predict(request.getFeatures());
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            log.info("单次预测完成，耗时: {}ms", processingTime);
            
            return ResponseEntity.ok(new PredictionResponse(
                prediction, 
                "预测成功", 
                processingTime
            ));
            
        } catch (Exception e) {
            log.error("预测失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new PredictionResponse(null, "预测失败: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/batch")
    public ResponseEntity<BatchPredictionResponse> predictBatch(@RequestBody BatchPredictionRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            
            List<Double> predictions = modelService.batchPredict(request.getFeaturesList());
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            log.info("批量预测完成，预测数量: {}, 耗时: {}ms", 
                predictions.size(), processingTime);
            
            return ResponseEntity.ok(new BatchPredictionResponse(
                predictions, 
                "批量预测成功", 
                processingTime
            ));
            
        } catch (Exception e) {
            log.error("批量预测失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BatchPredictionResponse(null, "批量预测失败: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("model", modelService != null ? "LOADED" : "NOT_LOADED");
        
        return ResponseEntity.ok(health);
    }
}
```

### 4.2 微服务架构设计

#### 配置文件
```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /ai-service

spring:
  application:
    name: ai-prediction-service
  
  # Redis配置（缓存预测结果）
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# 模型配置
model:
  cache:
    enabled: true
    ttl: 3600  # 缓存1小时
  batch:
    max-size: 1000
    timeout: 30000  # 30秒超时

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# 日志配置
logging:
  level:
    com.example.ai: DEBUG
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n'
```

### 4.3 模型版本管理

#### 模型版本控制器
```java
@RestController
@RequestMapping("/api/v1/models")
@Slf4j
public class ModelVersionController {
    
    @Autowired
    private ModelVersionService modelVersionService;
    
    @PostMapping("/versions")
    public ResponseEntity<String> deployNewVersion(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("version") String version) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("模型文件不能为空");
            }
            
            // 部署新版本
            modelVersionService.deployNewVersion(file, version);
            
            log.info("新模型版本部署成功: {}", version);
            return ResponseEntity.ok("模型版本 " + version + " 部署成功");
            
        } catch (Exception e) {
            log.error("模型部署失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("模型部署失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/rollback/{version}")
    public ResponseEntity<String> rollbackToVersion(@PathVariable String version) {
        try {
            modelVersionService.rollbackToVersion(version);
            log.info("模型回滚成功: {}", version);
            return ResponseEntity.ok("已回滚到版本 " + version);
            
        } catch (Exception e) {
            log.error("模型回滚失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("模型回滚失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/versions")
    public ResponseEntity<List<ModelVersion>> listVersions() {
        List<ModelVersion> versions = modelVersionService.listVersions();
        return ResponseEntity.ok(versions);
    }
}
```

---

## 📊 第五部分：实时数据处理

### 5.1 Apache Kafka集成

#### Kafka消费者示例
```java
@Component
@Slf4j
public class RealTimeDataProcessor {
    
    @Autowired
    private ModelService modelService;
    
    @KafkaListener(topics = "sensor-data", groupId = "ai-prediction-group")
    public void processRealTimeData(ConsumerRecord<String, String> record) {
        try {
            // 解析传感器数据
            ObjectMapper mapper = new ObjectMapper();
            SensorData sensorData = mapper.readValue(record.value(), SensorData.class);
            
            // 特征提取
            double[] features = extractFeatures(sensorData);
            
            // 实时预测
            double prediction = modelService.predict(features);
            
            // 异常检测
            if (prediction > sensorData.getThreshold()) {
                handleAnomaly(sensorData, prediction);
            }
            
            // 存储结果
            storePredictionResult(sensorData.getSensorId(), prediction, System.currentTimeMillis());
            
            log.debug("处理传感器数据: 传感器ID={}, 预测值={}", 
                sensorData.getSensorId(), prediction);
            
        } catch (Exception e) {
            log.error("处理实时数据失败: {}", record.value(), e);
        }
    }
    
    private double[] extractFeatures(SensorData data) {
        return new double[]{
            data.getTemperature(),
            data.getHumidity(),
            data.getPressure(),
            data.getVibration(),
            data.getCurrent()
        };
    }
    
    private void handleAnomaly(SensorData data, double prediction) {
        // 发送告警
        AlertMessage alert = new AlertMessage();
        alert.setSensorId(data.getSensorId());
        alert.setPrediction(prediction);
        alert.setThreshold(data.getThreshold());
        alert.setTimestamp(System.currentTimeMillis());
        alert.setSeverity("HIGH");
        
        // 发送到告警主题
        kafkaTemplate.send("alerts", alert);
        
        log.warn("检测到异常: 传感器ID={}, 预测值={}, 阈值={}", 
            data.getSensorId(), prediction, data.getThreshold());
    }
}
```

### 5.2 Apache Flink流处理

#### Flink作业示例
```java
public class RealTimeMLPipeline {
    
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);
        env.enableCheckpointing(5000);
        
        // Kafka数据源配置
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", "localhost:9092");
        kafkaProps.setProperty("group.id", "flink-ml-pipeline");
        
        // 创建Kafka消费者
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(
            "raw-data", 
            new SimpleStringSchema(), 
            kafkaProps
        );
        
        // 数据流处理管道
        DataStream<String> rawData = env.addSource(consumer);
        
        DataStream<SensorReading> sensorReadings = rawData
            .map(new JsonToSensorReadingMap())
            .filter(reading -> reading != null);
        
        // 窗口聚合
        DataStream<AggregatedFeatures> aggregatedFeatures = sensorReadings
            .keyBy(SensorReading::getSensorId)
            .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
            .aggregate(new FeatureAggregator());
        
        // 在线预测
        DataStream<PredictionResult> predictions = aggregatedFeatures
            .map(new MLPredictionFunction());
        
        // 异常检测
        DataStream<Alert> alerts = predictions
            .filter(new AnomalyDetectionFilter())
            .map(new AlertGenerationFunction());
        
        // 输出到Kafka
        FlinkKafkaProducer<Alert> alertProducer = new FlinkKafkaProducer<>(
            "alerts",
            new AlertSerializationSchema(),
            kafkaProps
        );
        
        alerts.addSink(alertProducer);
        
        // 执行作业
        env.execute("Real-time ML Pipeline");
    }
    
    // 特征聚合器
    public static class FeatureAggregator implements AggregateFunction<SensorReading, FeatureAccumulator, AggregatedFeatures> {
        
        @Override
        public FeatureAccumulator createAccumulator() {
            return new FeatureAccumulator();
        }
        
        @Override
        public FeatureAccumulator add(SensorReading reading, FeatureAccumulator accumulator) {
            accumulator.add(reading);
            return accumulator;
        }
        
        @Override
        public AggregatedFeatures getResult(FeatureAccumulator accumulator) {
            return accumulator.getAggregatedFeatures();
        }
        
        @Override
        public FeatureAccumulator merge(FeatureAccumulator a, FeatureAccumulator b) {
            return a.merge(b);
        }
    }
    
    // ML预测函数
    public static class MLPredictionFunction implements MapFunction<AggregatedFeatures, PredictionResult> {
        
        private transient MultiLayerNetwork model;
        
        @Override
        public void open(Configuration parameters) throws Exception {
            // 加载预训练模型
            model = MultiLayerNetwork.load(new File("/models/production_model.zip"), false);
        }
        
        @Override
        public PredictionResult map(AggregatedFeatures features) throws Exception {
            double[] inputFeatures = features.toArray();
            INDArray input = Nd4j.create(inputFeatures).reshape(1, inputFeatures.length);
            INDArray output = model.output(input);
            
            double prediction = output.getDouble(0);
            double confidence = calculateConfidence(output);
            
            return new PredictionResult(
                features.getSensorId(),
                prediction,
                confidence,
                System.currentTimeMillis()
            );
        }
        
        private double calculateConfidence(INDArray output) {
            // 计算预测置信度
            return Math.max(0.0, Math.min(1.0, 1.0 - Math.abs(0.5 - output.getDouble(0)) * 2));
        }
    }
}
```

---

## 🔍 第六部分：模型监控与运维

### 6.1 模型性能监控

#### 监控指标收集器
```java
@Component
@Slf4j
public class ModelMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Timer.Sample predictionTimer;
    private final Counter predictionCounter;
    private final Gauge accuracyGauge;
    
    public ModelMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.predictionCounter = Counter.builder("model.predictions.total")
            .description("模型预测总次数")
            .register(meterRegistry);
        
        this.accuracyGauge = Gauge.builder("model.accuracy")
            .description("模型准确率")
            .register(meterRegistry, this, ModelMetricsCollector::getCurrentAccuracy);
    }
    
    public void recordPrediction(double actualValue, double predictedValue, long responseTime) {
        // 记录预测次数
        predictionCounter.increment();
        
        // 记录响应时间
        Timer.builder("model.prediction.duration")
            .description("模型预测耗时")
            .register(meterRegistry)
            .record(responseTime, TimeUnit.MILLISECONDS);
        
        // 记录预测误差
        double error = Math.abs(actualValue - predictedValue);
        Gauge.builder("model.prediction.error")
            .description("模型预测误差")
            .register(meterRegistry, error, value -> value);
        
        // 更新准确率统计
        updateAccuracyStatistics(actualValue, predictedValue);
    }
    
    public void recordModelLoad(String modelVersion, long loadTime) {
        Timer.builder("model.load.duration")
            .tag("version", modelVersion)
            .description("模型加载耗时")
            .register(meterRegistry)
            .record(loadTime, TimeUnit.MILLISECONDS);
    }
    
    public void recordAnomalyDetection(String sensorId, double anomalyScore) {
        Counter.builder("model.anomalies.detected")
            .tag("sensor", sensorId)
            .description("检测到的异常数量")
            .register(meterRegistry)
            .increment();
        
        Gauge.builder("model.anomaly.score")
            .tag("sensor", sensorId)
            .description("异常分数")
            .register(meterRegistry, anomalyScore, value -> value);
    }
    
    private double getCurrentAccuracy() {
        // 计算当前模型准确率
        return accuracyCalculator.getCurrentAccuracy();
    }
    
    private void updateAccuracyStatistics(double actual, double predicted) {
        // 更新准确率统计信息
        accuracyCalculator.addPrediction(actual, predicted);
    }
}
```

### 6.2 模型漂移检测

#### 数据漂移检测器
```java
@Service
@Slf4j
public class ModelDriftDetector {
    
    private final ReferenceDataRepository referenceDataRepository;
    private final StatisticalTestService statisticalTestService;
    
    @Scheduled(fixedDelay = 3600000) // 每小时检测一次
    public void detectDrift() {
        try {
            // 获取参考数据分布
            DataDistribution referenceDistribution = referenceDataRepository.getReferenceDistribution();
            
            // 获取当前数据分布
            DataDistribution currentDistribution = getCurrentDataDistribution();
            
            // 执行漂移检测测试
            DriftTestResult result = performDriftTests(referenceDistribution, currentDistribution);
            
            if (result.isDriftDetected()) {
                handleModelDrift(result);
            }
            
            log.info("模型漂移检测完成: 漂移状态={}, P值={}", 
                result.isDriftDetected(), result.getPValue());
            
        } catch (Exception e) {
            log.error("模型漂移检测失败", e);
        }
    }
    
    private DriftTestResult performDriftTests(DataDistribution reference, DataDistribution current) {
        // Kolmogorov-Smirnov测试
        double ksStatistic = statisticalTestService.kolmogorovSmirnovTest(
            reference.getValues(), current.getValues()
        );
        
        // 卡方检验
        double chiSquareStatistic = statisticalTestService.chiSquareTest(
            reference.getHistogram(), current.getHistogram()
        );
        
        // Population Stability Index (PSI)
        double psi = calculatePSI(reference, current);
        
        // 综合判断
        boolean isDriftDetected = ksStatistic > 0.05 || chiSquareStatistic > 0.05 || psi > 0.2;
        
        return new DriftTestResult(isDriftDetected, ksStatistic, chiSquareStatistic, psi);
    }
    
    private double calculatePSI(DataDistribution reference, DataDistribution current) {
        double psi = 0.0;
        
        for (int i = 0; i < reference.getBins().length; i++) {
            double refPercent = reference.getBins()[i];
            double currPercent = current.getBins()[i];
            
            if (refPercent > 0 && currPercent > 0) {
                psi += (currPercent - refPercent) * Math.log(currPercent / refPercent);
            }
        }
        
        return psi;
    }
    
    private void handleModelDrift(DriftTestResult result) {
        // 发送告警
        DriftAlert alert = new DriftAlert();
        alert.setTimestamp(System.currentTimeMillis());
        alert.setKsStatistic(result.getKsStatistic());
        alert.setChiSquareStatistic(result.getChiSquareStatistic());
        alert.setPsi(result.getPsi());
        alert.setSeverity(determineSeverity(result));
        
        alertService.sendDriftAlert(alert);
        
        // 触发重训练流程
        if (result.getPsi() > 0.5) {
            triggerModelRetraining();
        }
        
        log.warn("检测到模型漂移: PSI={}, 建议重训练模型", result.getPsi());
    }
    
    private void triggerModelRetraining() {
        // 触发自动重训练
        retrainingService.scheduleRetraining();
        log.info("已触发模型重训练流程");
    }
}
```

### 6.3 A/B测试框架

#### A/B测试管理器
```java
@Service
@Slf4j
public class ABTestManager {
    
    private final Map<String, ModelVariant> activeTests = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    @PostConstruct
    public void initializeABTests() {
        // 加载活跃的A/B测试配置
        loadActiveTests();
    }
    
    public PredictionResult predict(String userId, double[] features) {
        // 确定用户应该使用哪个模型变体
        ModelVariant variant = selectModelVariant(userId);
        
        // 执行预测
        long startTime = System.currentTimeMillis();
        double prediction = variant.getModel().predict(features);
        long responseTime = System.currentTimeMillis() - startTime;
        
        // 记录实验数据
        recordExperimentData(userId, variant.getName(), prediction, responseTime);
        
        return new PredictionResult(prediction, variant.getName(), responseTime);
    }
    
    private ModelVariant selectModelVariant(String userId) {
        // 基于用户ID的一致性哈希分配
        int hash = Math.abs(userId.hashCode());
        
        for (ABTest test : getActiveTests()) {
            if (hash % 100 < test.getTrafficPercentage()) {
                if (random.nextDouble() < 0.5) {
                    return test.getVariantA();
                } else {
                    return test.getVariantB();
                }
            }
        }
        
        // 默认返回控制组模型
        return getDefaultModel();
    }
    
    private void recordExperimentData(String userId, String variant, double prediction, long responseTime) {
        ExperimentRecord record = new ExperimentRecord();
        record.setUserId(userId);
        record.setVariant(variant);
        record.setPrediction(prediction);
        record.setResponseTime(responseTime);
        record.setTimestamp(System.currentTimeMillis());
        
        experimentDataRepository.save(record);
    }
    
    @Scheduled(fixedDelay = 86400000) // 每天分析一次
    public void analyzeExperimentResults() {
        for (ABTest test : getActiveTests()) {
            ExperimentAnalysis analysis = performStatisticalAnalysis(test);
            
            if (analysis.isStatisticallySignificant()) {
                handleSignificantResult(test, analysis);
            }
            
            log.info("A/B测试分析完成: 测试={}, 显著性={}, 提升={}%", 
                test.getName(), 
                analysis.isStatisticallySignificant(),
                analysis.getImprovement() * 100);
        }
    }
    
    private ExperimentAnalysis performStatisticalAnalysis(ABTest test) {
        // 获取实验数据
        List<ExperimentRecord> variantAData = experimentDataRepository.findByVariant(test.getVariantA().getName());
        List<ExperimentRecord> variantBData = experimentDataRepository.findByVariant(test.getVariantB().getName());
        
        // 计算统计指标
        double meanA = variantAData.stream().mapToDouble(ExperimentRecord::getPrediction).average().orElse(0.0);
        double meanB = variantBData.stream().mapToDouble(ExperimentRecord::getPrediction).average().orElse(0.0);
        
        // T检验
        double tStatistic = calculateTStatistic(variantAData, variantBData);
        double pValue = calculatePValue(tStatistic, variantAData.size() + variantBData.size() - 2);
        
        boolean isSignificant = pValue < 0.05;
        double improvement = (meanB - meanA) / meanA;
        
        return new ExperimentAnalysis(isSignificant, improvement, pValue, tStatistic);
    }
    
    private void handleSignificantResult(ABTest test, ExperimentAnalysis analysis) {
        if (analysis.getImprovement() > 0.05) { // 5%以上的提升
            // 推广获胜变体
            promoteWinningVariant(test.getVariantB());
            log.info("A/B测试完成，推广获胜变体: {}", test.getVariantB().getName());
        }
        
        // 结束测试
        endABTest(test.getName());
    }
}
```

---

## 🏗️ 第七部分：生产环境部署

### 7.1 Docker容器化部署

#### Dockerfile
```dockerfile
# 多阶段构建
FROM maven:3.8.6-openjdk-11 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests

# 运行时镜像
FROM openjdk:11-jre-slim

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 复制应用文件
COPY --from=build /app/target/ai-service-*.jar app.jar
COPY --chown=appuser:appuser models/ ./models/
COPY --chown=appuser:appuser scripts/ ./scripts/

# 设置JVM参数
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication"

# 切换到非root用户
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose配置
```yaml
version: '3.8'

services:
  ai-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - DATABASE_URL=jdbc:postgresql://postgres:5432/aidb
    depends_on:
      - redis
      - kafka
      - postgres
    volumes:
      - ./logs:/app/logs
      - ./models:/app/models
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2'
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped

  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper
    volumes:
      - kafka_data:/var/lib/kafka/data

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data

  postgres:
    image: postgres:14
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: aidb
      POSTGRES_USER: aiuser
      POSTGRES_PASSWORD: aipassword
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards

volumes:
  redis_data:
  kafka_data:
  zookeeper_data:
  postgres_data:
  prometheus_data:
  grafana_data:
```

### 7.2 Kubernetes部署

#### 部署配置
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-service
  labels:
    app: ai-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-service
  template:
    metadata:
      labels:
        app: ai-service
    spec:
      containers:
      - name: ai-service
        image: ai-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: REDIS_HOST
          value: "redis-service"
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-service:9092"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: model-storage
          mountPath: /app/models
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: model-storage
        persistentVolumeClaim:
          claimName: model-pvc
      - name: logs
        emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: ai-service
spec:
  selector:
    app: ai-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 📈 第八部分：性能优化

### 8.1 JVM优化

#### 性能配置
```bash
# JVM调优参数
JAVA_OPTS="
-Xmx8g 
-Xms4g 
-XX:+UseG1GC 
-XX:MaxGCPauseMillis=200 
-XX:+UseStringDeduplication 
-XX:+OptimizeStringConcat 
-XX:+UseCompressedOops 
-XX:+UseCompressedClassPointers 
-Djava.awt.headless=true 
-Dfile.encoding=UTF-8
-XX:+HeapDumpOnOutOfMemoryError 
-XX:HeapDumpPath=/app/logs/heapdump.hprof
-Xloggc:/app/logs/gc.log 
-XX:+PrintGCDetails 
-XX:+PrintGCTimeStamps
"
```

### 8.2 模型优化

#### 模型压缩工具
```java
@Service
public class ModelOptimizer {
    
    public MultiLayerNetwork optimizeModel(MultiLayerNetwork originalModel) {
        // 模型量化
        MultiLayerNetwork quantizedModel = quantizeModel(originalModel);
        
        // 剪枝
        MultiLayerNetwork prunedModel = pruneModel(quantizedModel);
        
        // 知识蒸馏
        MultiLayerNetwork distilledModel = distillModel(prunedModel);
        
        return distilledModel;
    }
    
    private MultiLayerNetwork quantizeModel(MultiLayerNetwork model) {
        // 权重量化到8位
        MultiLayerConfiguration conf = model.getLayerWiseConfigurations();
        
        // 创建量化配置
        MultiLayerConfiguration quantizedConf = new NeuralNetConfiguration.Builder()
            .seed(conf.getSeed())
            .optimizationAlgo(conf.getOptimizationAlgo())
            .updater(conf.getUpdater())
            .weightInit(WeightInit.XAVIER)
            .list()
            .build();
        
        // 复制并量化权重
        MultiLayerNetwork quantizedModel = new MultiLayerNetwork(quantizedConf);
        quantizedModel.init();
        
        for (int i = 0; i < model.getnLayers(); i++) {
            INDArray weights = model.getLayer(i).getParams();
            INDArray quantizedWeights = quantizeWeights(weights);
            quantizedModel.getLayer(i).setParams(quantizedWeights);
        }
        
        return quantizedModel;
    }
    
    private INDArray quantizeWeights(INDArray weights) {
        // 简单的线性量化
        double min = weights.minNumber().doubleValue();
        double max = weights.maxNumber().doubleValue();
        double scale = (max - min) / 255.0;
        
        INDArray quantized = weights.sub(min).div(scale).round();
        return quantized.mul(scale).add(min);
    }
    
    private MultiLayerNetwork pruneModel(MultiLayerNetwork model) {
        // 基于权重大小的剪枝
        double pruningThreshold = 0.01;
        
        for (int i = 0; i < model.getnLayers(); i++) {
            INDArray weights = model.getLayer(i).getParams();
            INDArray mask = Nd4j.abs(weights).gt(pruningThreshold);
            INDArray prunedWeights = weights.mul(mask);
            model.getLayer(i).setParams(prunedWeights);
        }
        
        return model;
    }
}
```

### 8.3 缓存策略

#### 智能缓存管理
```java
@Service
@Slf4j
public class PredictionCacheManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final LoadingCache<String, Double> localCache;
    
    public PredictionCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build(this::computePrediction);
    }
    
    public double getCachedPrediction(String featureHash, double[] features) {
        try {
            // 先查本地缓存
            Double cached = localCache.getIfPresent(featureHash);
            if (cached != null) {
                log.debug("本地缓存命中: {}", featureHash);
                return cached;
            }
            
            // 再查Redis缓存
            cached = (Double) redisTemplate.opsForValue().get("prediction:" + featureHash);
            if (cached != null) {
                log.debug("Redis缓存命中: {}", featureHash);
                localCache.put(featureHash, cached);
                return cached;
            }
            
            // 缓存未命中，计算预测
            log.debug("缓存未命中，计算预测: {}", featureHash);
            return localCache.get(featureHash);
            
        } catch (Exception e) {
            log.error("缓存操作失败", e);
            return computePrediction(featureHash);
        }
    }
    
    private Double computePrediction(String featureHash) {
        // 实际的模型预测逻辑
        double[] features = parseFeatures(featureHash);
        double prediction = modelService.predict(features);
        
        // 缓存结果
        cachePrediction(featureHash, prediction);
        
        return prediction;
    }
    
    private void cachePrediction(String featureHash, double prediction) {
        try {
            // 缓存到Redis，设置过期时间
            redisTemplate.opsForValue().set(
                "prediction:" + featureHash, 
                prediction,