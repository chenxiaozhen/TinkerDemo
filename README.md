# TinkerDemo（微信Tinker热修复集成以及使用）

#### [Tinker项目](https://github.com/Tencent/tinker)(点击进入)
>Tinker是微信官方的Android热补丁解决方案，它支持动态下发代码。.so库以及资源库，让应用能够在不需要重复安装的情况下实现更新，当然也可以使用Tinker来更新你的插件。


在接入Tinker之前先对Tinker的结构了解一下

##### Tinker主要包括一下几个部分：
>1.gradle编译插件：tinker-patch-gradle-plugin。  
2.核心SDK库：tinker-android-lib。  
3.非gradle编译用户的命令行版本：tinker-patch-cil.jar。

##### Tinker的已知问题:
>1.Tinker不支持修改AndroidManifest.xml，Tinker不支持新增四大组件(1.9.0支持新增非export的Activity)；  
2.由于Google Play的开发者条款限制，不建议在GP渠道动态更新代码；  
3.在Android N上，补丁对应用启动时间有轻微的影响；
4.不支持部分三星android-21机型，加载补丁时会主动抛出"`TinkerRuntimeException:checkDexInstall failed`"；
5.对于资源替换，不支持修改remoteView。例如transition动画，notification icon以及桌面图标。

### 下面对Tinker进行接入

##### 步骤一：项目的build.gradle文件
```Java

	buildscript {
	    repositories {
	        jcenter()
	    }
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.2.3'
	
	        classpath "com.tencent.tinker:tinker-patch-gradle-plugin:${TINKER_VERSION}"
	        // NOTE: Do not place your application dependencies here; they belong
	        // in the individual module build.gradle files
	    }
	}
	
	allprojects {
	    repositories {
	        jcenter()
	    }
	}
	
	task clean(type: Delete) {
	    delete rootProject.buildDir
	}
```

##### 步骤二：app的build.gradle文件
以下这些只是基本测试通过的属性，Tinker官方github上面还有更多可选可设置的属性，如果还需要设置更多，请移步至 ![Tinker-接入指南](https://github.com/Tencent/tinker/wiki/Tinker-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97) 查看。

```Java

	apply plugin: 'com.android.application'
	
	android {
	    compileSdkVersion 25
	    buildToolsVersion "25.0.2"
	
	    // Tinker推荐模式
	    dexOptions {
	        jumboMode = true
	    }
	
	    // 关闭aapt对png优化
	    aaptOptions {
	        cruncherEnabled false
	    }
	
	    signingConfigs {
	        try {
	            config {
	                keyAlias 'testres'
	                keyPassword 'testres'
	                storeFile file('./keystore/release.keystore')
	                storePassword 'testres'
	            }
	        } catch (ex) {
	            throw new InvalidUserDataException(ex.toString())
	        }
	    }
	
	    defaultConfig {
	        applicationId "com.cxz.tinker"
	        minSdkVersion 15
	        targetSdkVersion 25
	        versionCode 1
	        versionName "1.0"
	        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
	
	        // 使用multiDex库
	        multiDexEnabled true
	        // 设置签名
	        signingConfig signingConfigs.config
	        manifestPlaceholders = [TINKER_ID: "${getTinkerIdValue()}"]
	        buildConfigField "String", "MESSAGE", "\"I am the base apk\""
	        buildConfigField "String", "CLIENTVERSION", "\"${getTinkerIdValue()}\""
	        buildConfigField "String", "PLATFORM",  "\"all\""
	    }
	    buildTypes {
	        release {
	            minifyEnabled false
	            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
	        }
	        debug {
	            minifyEnabled false
	            signingConfig signingConfigs.config
	        }
	    }
	    sourceSets {
	        main {
	            jniLibs.srcDirs = ['libs']
	        }
	    }
	}
	
	dependencies {
	    compile fileTree(dir: 'libs', include: ['*.jar'])
	    androidTestCompile('com.android.support.test.espresso:espresso-core:2.2.2', {
	        exclude group: 'com.android.support', module: 'support-annotations'
	    })
	    compile 'com.android.support:appcompat-v7:25.3.1'
	    testCompile 'junit:junit:4.12'
	
	    //可选，用于生成application类
	    provided ("com.tencent.tinker:tinker-android-anno:${TINKER_VERSION}"){changing = true}
	    //Tinker的核心库
	    compile ("com.tencent.tinker:tinker-android-lib:${TINKER_VERSION}"){changing = true}
	}
	
	// 指定JDK版本
	def getJavaVersion() {
	    return JavaVersion.VERSION_1_7
	}
	
	def bakPath = file("${buildDir}/bakApk/")
	
	/**
	 *
	 */
	ext {
	    tinkerEnabled = true
	    // 基础版本apk
	    tinkerOldApkPath = "${bakPath}/app-debug-20170911-09-39-26.apk"
	    //proguard mapping file to build patch apk
	    tinkerApplyMappingPath = "${bakPath}/"
	    // 与基础版本一起生成的R.text文件
	    tinkerApplyResourcePath = "${bakPath}/app-debug-20170911-09-39-26-R.txt"
	    //only use for build all flavor, if not, just ignore this field
	    tinkerBuildFlavorDirectory = "${bakPath}/"
	
	}
	
	// 基础APK的位置
	def getOldApkPath() {
	    return hasProperty("OLD_APK") ? OLD_APK : ext.tinkerOldApkPath
	}
	
	// Mapping的位置
	def getApplyMappingPath() {
	    return hasProperty("APPLY_MAPPING") ? APPLY_MAPPING : ext.tinkerApplyMappingPath
	}
	
	// ResourceMapping的位置
	def getApplyResourceMappingPath() {
	    return hasProperty("APPLY_RESOURCE") ? APPLY_RESOURCE : ext.tinkerApplyResourcePath
	}
	
	// 用来获取TinkerId(当前版本号就是TinkerId)
	def getTinkerIdValue() {
	    return android.defaultConfig.versionName
	}
	
	def buildWithTinker() {
	    return hasProperty("TINKER_ENABLE") ? TINKER_ENABLE : ext.tinkerEnabled
	}
	
	def getTinkerBuildFlavorDirectory() {
	    return ext.tinkerBuildFlavorDirectory
	}
	
	if (buildWithTinker()) {
	    // Tinker插件
	    apply plugin: 'com.tencent.tinker.patch'
	    /**
	     * 全局信息相关配置
	     */
	    tinkerPatch {
	        // 基准apk包的路径，必须输入，否则会报错。
	        oldApk = getOldApkPath()
	        /**
	         * 如果出现以下的情况，并且ignoreWarning为false，我们将中断编译。
	         * 因为这些情况可能会导致编译出来的patch包带来风险：
	         * case 1: minSdkVersion小于14，但是dexMode的值为"raw";
	         * case 2: 新编译的安装包出现新增的四大组件(Activity, BroadcastReceiver...)；
	         * case 3: 定义在dex.loader用于加载补丁的类不在main dex中;
	         * case 4:  定义在dex.loader用于加载补丁的类出现修改；
	         * case 5: resources.arsc改变，但没有使用applyResourceMapping编译。
	         */
	        ignoreWarning = false
	
	        /**
	         * 运行过程中需要验证基准apk包与补丁包的签名是否一致，是否需要签名。
	         */
	        useSign = true
	
	        /**
	         * optional，default 'true'
	         * whether use tinker to build
	         */
	        tinkerEnable = buildWithTinker()
	
	        /**
	         * 编译相关的配置项
	         */
	        buildConfig {
	            /**
	             * 可选参数；在编译新的apk时候，我们希望通过保持旧apk的proguard混淆方式，从而减少补丁包的大小。
	             * 这个只是推荐设置，不设置applyMapping也不会影响任何的assemble编译。
	             */
	            applyMapping = getApplyMappingPath()
	            /**
	             * 可选参数；在编译新的apk时候，我们希望通过旧apk的R.txt文件保持ResId的分配。
	             * 这样不仅可以减少补丁包的大小，同时也避免由于ResId改变导致remote view异常。
	             */
	            applyResourceMapping = getApplyResourceMappingPath()
	
	            /**
	             * 在运行过程中，我们需要验证基准apk包的tinkerId是否等于补丁包的tinkerId。
	             * 这个是决定补丁包能运行在哪些基准包上面，一般来说我们可以使用git版本号、versionName等等。
	             */
	            tinkerId = getTinkerIdValue()
	
	            /**
	             * 如果我们有多个dex,编译补丁时可能会由于类的移动导致变更增多。
	             * 若打开keepDexApply模式，补丁包将根据基准包的类分布来编译。
	             */
	            keepDexApply = false
	        }
	        /**
	         * dex相关的配置项
	         */
	        dex {
	            /**
	             * 只能是'raw'或者'jar'。
	             * 对于'raw'模式，我们将会保持输入dex的格式。
	             * 对于'jar'模式，我们将会把输入dex重新压缩封装到jar。
	             * 如果你的minSdkVersion小于14，你必须选择‘jar’模式，而且它更省存储空间，但是验证md5时比'raw'模式耗时。
	             * 默认我们并不会去校验md5,一般情况下选择jar模式即可。
	             */
	            dexMode = "jar"
	
	            /**
	             * 需要处理dex路径，支持*、?通配符，必须使用'/'分割。路径是相对安装包的，例如assets/...
	             */
	            pattern = ["classes*.dex",
	                       "assets/secondary-dex-?.jar"]
	            /**
	             * 这一项非常重要，它定义了哪些类在加载补丁包的时候会用到。
	             * 这些类是通过Tinker无法修改的类，也是一定要放在main dex的类。
	             * 这里需要定义的类有：
	             * 1. 你自己定义的Application类；
	             * 2. Tinker库中用于加载补丁包的部分类，即com.tencent.tinker.loader.*；
	             * 3. 如果你自定义了TinkerLoader，需要将它以及它引用的所有类也加入loader中；
	             * 4. 其他一些你不希望被更改的类，例如Sample中的BaseBuildInfo类。
	             *    这里需要注意的是，这些类的直接引用类也需要加入到loader中。
	             *    或者你需要将这个类变成非preverify。
	             * 5. 使用1.7.6版本之后版本，参数1、2会自动填写。
	             *
	             */
	            loader = [
	                    // Tinker库中用于加载补丁包的部分类
	                    "com.tencent.tinker.loader.*",
	                    // 自己定义的Application类；
	                    "com.tinker.app.AppContext",
	                    //use sample, let BaseBuildInfo unchangeable with tinker
	                    "tinker.sample.android.app.BaseBuildInfo"
	            ]
	        }
	        /**
	         * lib相关的配置项
	         */
	        lib {
	            /**
	             * 需要处理lib路径，支持*、?通配符，必须使用'/'分割。
	             * 与dex.pattern一致, 路径是相对安装包的，例如assets/...
	             */
	            pattern = ["lib/*/*.so"]
	        }
	        /**
	         * res相关的配置项
	         */
	        res {
	            /**
	             * 需要处理res路径，支持*、?通配符，必须使用'/'分割。
	             * 与dex.pattern一致, 路径是相对安装包的，例如assets/...，
	             * 务必注意的是，只有满足pattern的资源才会放到合成后的资源包。
	             */
	            pattern = ["res/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]
	
	            /**
	             * 支持*、?通配符，必须使用'/'分割。若满足ignoreChange的pattern，在编译时会忽略该文件的新增、删除与修改。
	             * 最极端的情况，ignoreChange与上面的pattern一致，即会完全忽略所有资源的修改。
	             */
	            ignoreChange = ["assets/sample_meta.txt"]
	
	            /**
	             * 对于修改的资源，如果大于largeModSize，我们将使用bsdiff算法。
	             * 这可以降低补丁包的大小，但是会增加合成时的复杂度。默认大小为100kb
	             */
	            largeModSize = 100
	        }
	        /**
	         * 用于生成补丁包中的'package_meta.txt'文件
	         */
	        packageConfig {
	            /**
	             * configField("key", "value"), 默认我们自动从基准安装包与新安装包的Manifest中读取tinkerId,并自动写入configField。
	             * 在这里，你可以定义其他的信息，在运行时可以通过TinkerLoadResult.getPackageConfigByName得到相应的数值。
	             * 但是建议直接通过修改代码来实现，例如BuildConfig。
	             */
	            configField("patchMessage", "tinker is sample to use")
	            /**
	             * just a sample case, you can use such as sdkVersion, brand, channel...
	             * you can parse it in the SamplePatchListener.
	             * Then you can use patch conditional!
	             */
	            configField("platform", "all")
	            /**
	             * 配置patch补丁版本
	             */
	            configField("patchVersion", "1.0.0")
	        }
	        /**
	         * 7zip路径配置项，执行前提是useSign为true
	         */
	        sevenZip {
	            /**
	             * 例如"com.tencent.mm:SevenZip:1.1.10"，将自动根据机器属性获得对应的7za运行文件，推荐使用。
	             */
	            zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
	            /**
	             * 系统中的7za路径，例如"/usr/local/bin/7za"。path设置会覆盖zipArtifact，若都不设置，将直接使用7za去尝试。
	             */
	            // path = "/usr/local/bin/7za"
	        }
	    }
	
	    List<String> flavors = new ArrayList<>();
	    project.android.productFlavors.each { flavor ->
	        flavors.add(flavor.name)
	    }
	    boolean hasFlavors = flavors.size() > 0
	    /**
	     * bak apk and mapping
	     */
	    android.applicationVariants.all { variant ->
	        /**
	         * task type, you want to bak
	         */
	        def taskName = variant.name
	        def date = new Date().format("yyyyMMdd-HH-mm-ss")
	
	        tasks.all {
	            if ("assemble${taskName.capitalize()}".equalsIgnoreCase(it.name)) {
	
	                it.doLast {
	                    copy {
	                        def fileNamePrefix = "${project.name}-${variant.baseName}"
	                        def newFileNamePrefix = hasFlavors ? "${fileNamePrefix}" : "${fileNamePrefix}-${date}"
	
	                        def destPath = hasFlavors ? file("${bakPath}/${project.name}-${date}/${variant.flavorName}") : bakPath
	                        from variant.outputs.outputFile
	                        into destPath
	                        rename { String fileName ->
	                            fileName.replace("${fileNamePrefix}.apk", "${newFileNamePrefix}.apk")
	                        }
	
	                        from "${buildDir}/outputs/mapping/${variant.dirName}/mapping.txt"
	                        into destPath
	                        rename { String fileName ->
	                            fileName.replace("mapping.txt", "${newFileNamePrefix}-mapping.txt")
	                        }
	
	                        from "${buildDir}/intermediates/symbols/${variant.dirName}/R.txt"
	                        into destPath
	                        rename { String fileName ->
	                            fileName.replace("R.txt", "${newFileNamePrefix}-R.txt")
	                        }
	                    }
	                }
	            }
	        }
	    }
	    project.afterEvaluate {
	        //sample use for build all flavor for one time
	        if (hasFlavors) {
	            task(tinkerPatchAllFlavorRelease) {
	                group = 'tinker'
	                def originOldPath = getTinkerBuildFlavorDirectory()
	                for (String flavor : flavors) {
	                    def tinkerTask = tasks.getByName("tinkerPatch${flavor.capitalize()}Release")
	                    dependsOn tinkerTask
	                    def preAssembleTask = tasks.getByName("process${flavor.capitalize()}ReleaseManifest")
	                    preAssembleTask.doFirst {
	                        String flavorName = preAssembleTask.name.substring(7, 8).toLowerCase() + preAssembleTask.name.substring(8, preAssembleTask.name.length() - 15)
	                        project.tinkerPatch.oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release.apk"
	                        project.tinkerPatch.buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-mapping.txt"
	                        project.tinkerPatch.buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-R.txt"
	
	                    }
	
	                }
	            }
	
	            task(tinkerPatchAllFlavorDebug) {
	                group = 'tinker'
	                def originOldPath = getTinkerBuildFlavorDirectory()
	                for (String flavor : flavors) {
	                    def tinkerTask = tasks.getByName("tinkerPatch${flavor.capitalize()}Debug")
	                    dependsOn tinkerTask
	                    def preAssembleTask = tasks.getByName("process${flavor.capitalize()}DebugManifest")
	                    preAssembleTask.doFirst {
	                        String flavorName = preAssembleTask.name.substring(7, 8).toLowerCase() + preAssembleTask.name.substring(8, preAssembleTask.name.length() - 13)
	                        project.tinkerPatch.oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug.apk"
	                        project.tinkerPatch.buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-mapping.txt"
	                        project.tinkerPatch.buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-R.txt"
	                    }
	
	                }
	            }
	        }
	    }
	}
```

##### 步骤三：gradle.properties文件

将下面这行 Tinker 的版本号添加到 gradle.properties 文件中（Tinker的最新版本，请留意 ![Tinker github](https://github.com/Tencent/tinker)）
`TINKER_VERSION=1.8.0`

##### 步骤四：application文件

新建一个类，名字叫 SampleApplicationLike （随意起），并继承 DefaultApplicationLike ，注意这里不是继承 Application ，这个是 Tinker 的推荐写法。其他的注解和重写的方法，照着写就好了。最后自己的 Application 逻辑就写在 onCreate() 方法里面。

```Java

	package com.cxz.tinker;
	
	import android.annotation.TargetApi;
	import android.app.Application;
	import android.content.Context;
	import android.content.Intent;
	import android.os.Build;
	import android.support.multidex.MultiDex;
	
	import com.cxz.tinker.utils.ApplicationContext;
	import com.cxz.tinker.utils.TinkerManager;
	import com.tencent.tinker.anno.DefaultLifeCycle;
	import com.tencent.tinker.loader.app.DefaultApplicationLike;
	import com.tencent.tinker.loader.shareutil.ShareConstants;
	
	@DefaultLifeCycle(
	        application = "com.cxz.tinker.SampleApplication",
	        flags = ShareConstants.TINKER_ENABLE_ALL,
	        loadVerifyFlag = false
	)
	public class SampleApplicationLike extends DefaultApplicationLike {
	
	    public SampleApplicationLike(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
	        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
	    }
	
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        // 编写自己Application的业务
	
	    }
	
	    @Override
	    public void onBaseContextAttached(Context base) {
	        super.onBaseContextAttached(base);
	        //必须安装MultiDex的更新!
	        MultiDex.install(base);
	        //安装Tinker后加载multiDex
	        TinkerManager.installTinker(this);
	
	        ApplicationContext.application = getApplication();
	        ApplicationContext.context = getApplication();
	
	        TinkerManager.setTinkerApplicationLike(this);
	        TinkerManager.initFastCrashProtect();
	        TinkerManager.setUpgradeRetryEnable(true);
	    }
	
	    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
	        getApplication().registerActivityLifecycleCallbacks(callback);
	    }
	
	}

```

##### 步骤五：AndroidManifest.xml文件

1.在application标签里加入步骤四中新建的Application类 android:name=".SampleApplication"，此处的名字需要与步骤四的SampleApplicationLike 类最顶部的@DefaultLifeCycle()注解保持一致。如果你添加不进去，或者是红色的话，请先build一下。如下红色圈中：

![](/art/01.png)

2.注册一个处理加载补丁结果的service（SampleResultService）

```Java

	<service
  	          android:name=".service.SampleResultService"
            android:exported="false" />
```

3.Tinker需要在AndroidManifest.xml中指定TINKER_ID

```Java

	<meta-data
            android:name="TINKER_ID"
            android:value="${TINKER_ID}" />
```

4.加入访问sdcard权限，Android6.0以上的。

```Java

	<?xml version="1.0" encoding="utf-8"?>
	<manifest xmlns:android="http://schemas.android.com/apk/res/android"
	    package="com.cxz.tinker">
	
	    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
	
	    <application
	        android:name="com.cxz.tinker.SampleApplication"
	        android:allowBackup="true"
	        android:icon="@mipmap/ic_launcher"
	        android:label="@string/app_name"
	        android:supportsRtl="true"
	        android:theme="@style/AppTheme">
	        <activity android:name=".MainActivity">
	            <intent-filter>
	                <action android:name="android.intent.action.MAIN" />
	
	                <category android:name="android.intent.category.LAUNCHER" />
	            </intent-filter>
	        </activity>
	
	        <service
	            android:name=".service.SampleResultService"
	            android:exported="false" />
	        <meta-data
	            android:name="TINKER_ID"
	            android:value="${TINKER_ID}" />
	
	    </application>
	</manifest>
```

到此，Tinker接入配置结束了。

### 下面介绍如何简单使用Tinker

##### 步骤一：运行apk，运行完成后将配置文件的基准包改成新的

![](/art/03.png)

![](/art/05.png)

##### 步骤二：修改项目中的代码，然后就可以调用tinkerPatch命令生成patch补丁文件，tinkerPatch有Debug和Release两种模式，任选一种运行测试即可。

![](/art/02.png)

![](/art/04.png)

##### 步骤三：生成的patch文件（`patch_signed_7zip.apk`），拷贝到相应的路径下

![](/art/07.png)

加载patch文件成功后重启应用如下：

![](/art/06.png)