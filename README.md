# S3 File Line Rewriter

![Build Status](https://img.shields.io/github/workflow/status/GuiaBolso/s3-file-line-rewriter/Check)
[![GitHub](https://img.shields.io/github/license/GuiaBolso/s3-file-line-rewriter)](https://github.com/GuiaBolso/s3-file-line-rewriter/blob/master/LICENSE)
[![Bintray Download](https://img.shields.io/bintray/v/gb-opensource/maven/s3-file-line-rewriter)](https://bintray.com/gb-opensource/maven/s3-file-line-rewriter)

## Introduction

With many countries creating law to protect user's privacy and data - such as European GDPR or Brazilian LGPD - companies are rushing to find a way to comply to these regulations.

When storing multiple user's data in the same S3 file and we want to wipe a single user, we have to open the S3 File, remove all lines with data from a specific user and then reupload the file with that data removed or anonymized.

The **S3 File Line Rewriter** library aims to ease the process of rewriting those files, with a clean API that will do everything with little memory footprint by processing everything with streams.

## Using with Gradle

This library is published to `Bintray jcenter`, so you'll need to configure that in your repositories:
```kotlin
repositories {
    mavenCentral()
    jcenter()
}
```

And then you can import it into your dependencies:
```kotlin
dependencies {
    implementation("br.com.guiabolso:s3-file-line-rewriter:{version}")
}
```

## Usage

Using this library is very easy:

#### Declaring the Rewriter:
```kotlin
val rewriter = S3FileLineRewriter(myAmazonS3Client)
```

#### Rewriting lines from a single file:
```kotlin
rewriter.rewriteFile(
    bucket = "bucket",
    key = "key"
) { lines: Sequence<String> -> 
    lines.map { it.replace("StringIWantToRedact", "*****") } 
}
```

#### Rewriting lines from every file with a specific prefix:
```kotlin
rewriter.rewriteAll(
    bucket = "bucket",
    prefix = "MyDirectory/SubDirectory"
) { lines: Sequence<String> -> 
    lines.map { it.replace("StringIWantToRedact", "*****") } 
}
```

## Advanced Usage

#### Line Breaks
By default, when rewriting, this library will use `\n` as the new line character. This can be changed by the System Property `br.com.guiabolso.s3filelinerewriter.newline`

#### Empty Lines
By default, when rewriting, if a line becomes empty after transforming it, this library will remove the empty lines from the file. This can be changed by the System Property `br.com.guiabolso.s3filelinerewriter.removeblank`


## Contributing
If you have any improvements, please feel free to file a PR!
