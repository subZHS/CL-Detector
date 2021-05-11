# CL-Detector

Based on [MuDetect](https://github.com/stg-tud/MUDetect), a pattern-based misuse detector, we implement our API-misuse detector CL-Detector using API constraints from both client code and library code. 

From library code, we apply several well-designed strategies to infer various constraints.

From client code, we extract API usage graphs (AUGs) and mine frequent usage patterns as constraints from client code. 

Then we combine the client-based constraints with library-based constraints extracted by L-Extractor. To obtain a rich set of constraints, our combination algorithm merges the AUG sets of both-side constraints, and then extends the constraint set by generating more AUGs through modifying existing AUGs. 

Finally, our detector uses both-side constraints to detect misuses such as missing call, missing condition checking, and missing exception handling.

## Environment

- Java 1.8.0_161
- Maven 3.5.3

## Prerequisites

- Edit some config parameters in the configuration file [config.properties](./mubench/src/main/resources/config.properties) to fit your local environment.
  - The config parameter libRootDir is the directory containing library source code of target APIs. Please download source code of all libraries of specific versions accordding to [targetAPIs.xlsx](../dataset/targetAPIs.xlsx).
  - The config parameter trainProjectBasePath is the directory containing client projects with top API occurrences mined from Boa tool. Please download client projects accordding to [ClientProjectsByBoa.zip](../dataset/ClientProjectsByBoa.zip).
  - The config parameter targetProjectBasePath is the directory of source code of projects in MuBench dataset. Please download target projects in MuBench accordding to [MuBench.csv](../dataset/MuBench.csv).
- Modify the files in checkouts-xp directory accordding to actual dataset. These files include the csv file containing constraints from library code, the csv file collecting misuses in MuBench, and client project lists of each API for mining constraints from client code. 
- For a new target API set, run [L-Extractor](./L-Extractor) to infer constraints from library code.

## Use

1. `cd` to this repo.

2. maven build:

```
mvn clean;mvn compile;mvn package -DskipTests
```

3. Run：

   - run our detector on MuBench dataset:

   ```
   java -jar mubench/target/DatasetMuDetectXP.jar
   ```

   - Run our detector on specific target project:

   ```
   java -jar mubench/target/MuDetectXP.jar detector_mode "0" target_src_path "[the target project source path]" dep_classpath "[the target project dependency classpath]"
   ```

## Results

- The patternResults directory contains the mined constraints from client code for each target API.
- The results directory contains misuse detection results for each target project. 
  - The findings-output.yml file contains locations of all detected misuses. 
  - The run-info-output.yml file contains some statistics of intermediate data and result data about the detection process.