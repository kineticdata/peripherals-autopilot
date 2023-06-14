
import java.text.SimpleDateFormat
import java.util.Date
plugins {
  java
  `maven-publish`
  id("net.nemerosa.versioning") version "2.14.0"
}

repositories {
  mavenLocal()
  maven {
    url = uri("https://s3.amazonaws.com/maven-repo-public-kineticdata.com/releases")
  }

  maven {
    url = uri("s3://maven-repo-private-kineticdata.com/releases")
    authentication {
      create<AwsImAuthentication>("awsIm")
    }

  }

  maven {
    url = uri("https://s3.amazonaws.com/maven-repo-public-kineticdata.com/snapshots")
  }

  maven {
    url = uri("s3://maven-repo-private-kineticdata.com/snapshots")
    authentication {
      create<AwsImAuthentication>("awsIm")
    }
  }

  maven {
    url = uri("https://repo.maven.apache.org/maven2/")
  }

  maven {
    url = uri("https://repo.springsource.org/release/")
  }


}

dependencies {
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.slf4j:slf4j-api:1.7.10")
  implementation("com.googlecode.json-simple:json-simple:1.1.1")
  implementation("com.kineticdata.agent:kinetic-agent-adapter:1.1.3")
  implementation("commons-lang:commons-lang:2.6")
  implementation("javax.json:javax.json-api:1.1")
  implementation("com.jayway.jsonpath:json-path:2.8.0")
  implementation("org.glassfish:javax.json:1.1")
  implementation("org.apache.commons:commons-exec:1.1")
  implementation("commons-collections:commons-collections:3.2.2")
  testImplementation("org.apache.logging.log4j:log4j-api:2.16.0")
  testImplementation("org.apache.logging.log4j:log4j-core:2.17.1")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
  testImplementation("com.fasterxml.jackson.core:jackson-core:2.9.6")
  testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.9.6")
  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.6.1")
}

group = "com.kineticdata.bridges.adapter"
version = "1.1.1-SNAPSHOT"
description = "kinetic-bridgehub-adapter-autopilot"
java.sourceCompatibility = JavaVersion.VERSION_1_8

publishing {
  publications.create<MavenPublication>("maven") {
    from(components["java"])
  }
  repositories {
    maven {
      val releasesUrl = uri("s3://maven-repo-private-kineticdata.com/releases")
      val snapshotsUrl = uri("s3://maven-repo-private-kineticdata.com/snapshots")
      url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
      authentication {
        create<AwsImAuthentication>("awsIm")
      }
    }
  }
}

tasks.withType<JavaCompile>() {
  options.encoding = "UTF-8"
}
versioning {
  gitRepoRootDir = "../../"
}
tasks.processResources {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
  from("src/main/resources"){
    filesMatching("**/*.version") {    
      expand(    
        "buildNumber" to versioning.info.build,
        "buildDate" to currentDate,    
        "timestamp" to System.currentTimeMillis(),    
        "version" to project.version    
      )    
    }
  }
}