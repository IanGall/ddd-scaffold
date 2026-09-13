def integrationTestBaseDir = basedir instanceof File ? basedir : new File(basedir.toString())
def project = new File(integrationTestBaseDir, "project/ian-ddd-smoke")

// 生成工程只有四个模块：RPC 契约不再由服务工程自带（统一放共享契约仓 ian-ddd-api）
assert !new File(project, "ian-ddd-smoke-api").exists()
assert new File(project, "ian-ddd-smoke-domain").isDirectory()
assert new File(project, "ian-ddd-smoke-infrastructure").isDirectory()
assert new File(project, "ian-ddd-smoke-trigger").isDirectory()
assert new File(project, "ian-ddd-smoke-boot").isDirectory()
assert !new File(project, "ian-ddd-smoke-application").exists()
def domainSource = new File(project, "ian-ddd-smoke-domain/src/main/java/cn/iantech/smoke")
def casesSource = new File(domainSource, "cases")
def coreDomainSource = new File(domainSource, "domain")
assert new File(casesSource, "user/service/UserCaseService.java").isFile()
assert new File(coreDomainSource, "user/service/UserDomainService.java").isFile()
assert new File(coreDomainSource, "support/infra/IEventProducer.java").isFile()
assert !new File(coreDomainSource, "cases").exists()
assert !new File(coreDomainSource, "support/repository").exists()
assert !new File(coreDomainSource, "support/adapter").exists()
assert !new File(coreDomainSource, "user/repository").exists()
assert !new File(coreDomainSource, "user/adapter").exists()
assert !new File(coreDomainSource, "xxx/repository").exists()
assert !new File(coreDomainSource, "xxx/adapter").exists()

def javaSources = { File sourceRoot ->
    def sources = []
    if (sourceRoot.isDirectory()) {
        sourceRoot.eachFileRecurse { file ->
            if (file.isFile() && file.name.endsWith(".java")) {
                sources << file
            }
        }
    }
    sources
}

javaSources(casesSource).each { source ->
    def text = source.text
    assert !(text =~ /import\s+cn\.iantech\.smoke\.(context|rpc|trigger|infrastructure)(\.|;)/)
    // Cases 不得依赖跨进程契约 DTO（契约已统一放共享契约仓的 cn.iantech.api.*）
    assert !(text =~ /import\s+cn\.iantech\.api(\.|;)/)
    assert !(text =~ /import\s+(cn\.iantech\.context|org\.apache\.dubbo)(\.|;)/)
}
javaSources(coreDomainSource).each { source ->
    assert !(source.text =~ /import\s+cn\.iantech\.smoke\.cases(\.|;)/)
    source.readLines()
            .findAll { line -> line.trim().startsWith("import org.springframework.") }
            .each { springImport ->
                assert source.path.contains("/service/")
                assert springImport.trim() == "import org.springframework.stereotype.Service;"
            }
}
def userDomainService = new File(coreDomainSource, "user/service/UserDomainService.java").text
assert userDomainService.contains("@Service")
def userCaseService = new File(casesSource, "user/service/UserCaseService.java").text
assert userCaseService.contains("private final UserDomainService userDomainService;")
assert userCaseService.contains("return userDomainService.queryUserInfo(request);")
// 骨架不预置中间件/平台专属能力：以下内容不应出现在生成工程中
assert !new File(coreDomainSource, "xxx").exists()
assert !new File(project, "ian-ddd-smoke-infrastructure/src/main/java/cn/iantech/smoke/infrastructure/channel").exists()
assert !new File(project, "ian-ddd-smoke-infrastructure/src/main/java/cn/iantech/smoke/infrastructure/event").exists()
assert !new File(project, "ian-ddd-smoke-boot/src/main/resources/sharding").exists()
assert !new File(project, "ian-ddd-smoke-trigger/src/main/java/cn/iantech/smoke/trigger/job").exists()
assert !new File(project, "ian-ddd-smoke-trigger/src/main/java/cn/iantech/smoke/trigger/listener").exists()
// 层语义锚点包必须保留：说明适配器放哪里（infrastructure 出站、trigger 入站）
assert new File(project, "ian-ddd-smoke-infrastructure/src/main/java/cn/iantech/smoke/infrastructure/package-info.java").isFile()
assert new File(project, "ian-ddd-smoke-trigger/src/main/java/cn/iantech/smoke/trigger/package-info.java").isFile()
assert new File(project, "ian-ddd-smoke-trigger/src/main/java/cn/iantech/smoke/trigger/rpc/package-info.java").isFile()

def triggerPom = new File(project, "ian-ddd-smoke-trigger/pom.xml").text
assert triggerPom.contains("<artifactId>ian-ddd-smoke-domain</artifactId>")
assert !triggerPom.contains("<artifactId>ian-ddd-smoke-application</artifactId>")
assert !triggerPom.contains("<artifactId>ian-ddd-smoke-api</artifactId>")

def bootPom = new File(project, "ian-ddd-smoke-boot/pom.xml").text
assert bootPom.contains("<artifactId>ian-ddd-smoke-domain</artifactId>")
assert !bootPom.contains("<artifactId>ian-ddd-smoke-application</artifactId>")
assert bootPom.contains("<artifactId>ddd-test-starter</artifactId>")
def infrastructurePom = new File(project, "ian-ddd-smoke-infrastructure/pom.xml").text
assert infrastructurePom.contains("<artifactId>ddd-redis-starter</artifactId>")
assert infrastructurePom.contains("<artifactId>ddd-id-generator-starter</artifactId>")
assert !infrastructurePom.contains("<artifactId>redisson-spring-boot-starter</artifactId>")
assert !infrastructurePom.contains("<artifactId>spring-boot-starter-kafka</artifactId>")
// 中间件与分片依赖不应随骨架下发
assert !bootPom.contains("shardingsphere")
assert !bootPom.contains("<artifactId>xxl-job-core</artifactId>")
assert !bootPom.contains("<artifactId>spring-boot-starter-kafka</artifactId>")
assert !triggerPom.contains("<artifactId>xxl-job-core</artifactId>")
assert !triggerPom.contains("<artifactId>spring-boot-starter-kafka</artifactId>")
def devConfiguration = new File(project, "ian-ddd-smoke-boot/src/main/resources/application-dev.yml").text
assert !devConfiguration.contains("kafka:")
assert !devConfiguration.contains("channel:")
assert !devConfiguration.contains("shardingsphere")
assert devConfiguration.contains("com.mysql.cj.jdbc.Driver")
assert !new File(project, "ian-ddd-smoke-infrastructure/src/main/java/cn/iantech/smoke/infrastructure/redis/IRedisService.java").exists()
assert !new File(project, "ian-ddd-smoke-infrastructure/src/main/java/cn/iantech/smoke/infrastructure/redis/RedissonService.java").exists()

def testConfiguration = new File(project,
        "ian-ddd-smoke-boot/src/main/resources/application-test.yml").text
assert testConfiguration.contains("id-generator:")
assert testConfiguration.contains("enabled: false")
def generatedReadme = new File(project, "README.md").text
assert generatedReadme.contains("cn.iantech.id.GlobalIdGenerator")
