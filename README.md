# STARS experiments based on data from model race cars

This repository analyzes driving data recorded using the [STARS
framework](https://github.com/tudo-aqua/stars). The data was recorded using the [AuNa](https://github.com/HarunTeper/AuNa)
Repository.

## Setup

The analysis requires the recorded data. To receive the data, there are two options:
1. Set `DOWNLOAD_EXPERIMENTS_DATA` in `experimentsConfiguration.kt` to `true`. This will automatically download and
   unzip the necessary data.
2. Manually download the data.
    1. Go to the [TU Dortmund Sciebo artifact](https://tu-dortmund.sciebo.de/s/gHctg8boFkKgcCF) where the experiment data is stored
    2. Download the `stars-auna-json-files.zip`
    3. Place the Zip-File into the root folder of this project.

**Remark:** The downloaded data has a size of approximately 60MB. The downloaded zip-file will be extracted during
the analysis. Make sure, that you have at least 600MB of free space.

## Running the Analysis

This project is a Gradle project with a shipped gradle wrapper. To execute the analysis simply execute:

- Linux/Mac: `./gradlew run`
- Windows: `./gradlew.bat run`

## Analysis Results

After the analysis is finished you can find the results in the `analysis-result-logs` sub-folder which will be
created during the analysis.

For each execution of the analysis pipeline a sub-folder with the start date and time ist created. In it, each metric
of the analysis has its own sub-folder. The analysis separates the results of each metric into different categories
with different detail levels.
- `*-severe.txt` lists all failed metric results
- `*-warning.txt` lists all warnings that occurred during analysis
- `*-info.txt` contains the summarized result of the metric
- `*-fine.txt` contains a more detailed result of the metric
- `*-finer.txt` contains all possible results of the metric
- `*-finest.txt` contains all possible results of the metric including meta information