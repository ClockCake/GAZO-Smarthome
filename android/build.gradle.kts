buildscript {
    // Option 1: set by key
    extra["biz_bom_version"] = "6.2.16"
    extra["sdk_version"] = "6.2.2"
}
allprojects {
    repositories {
        jcenter()
        maven {
            url = uri("https://maven-other.tuya.com/repository/maven-releases/")
        }
        maven {
            url = uri("https://maven-other.tuya.com/repository/maven-commercial-releases/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://central.maven.org/maven2/")
        }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://developer.huawei.com/repo/")
        }
        maven {
            url = uri("https://maven-other.tuya.com/repository/maven-commercial-releases/")
        }
    }
}
val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}