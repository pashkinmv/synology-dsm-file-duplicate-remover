# synology-dsm-file-duplicate-remover

<img src="https://travis-ci.org/aquenneville/syno-file-duplicate-remover.svg?branch=master"/>
The reason for this project was I could not delete all the duplicate files in my report in one click. 

I created this utility to delete the files duplicate by keeping the first version found. 

# Enabling the duplicate file reporting

- From the main menu > click the Storage Analyzer
- Under Report profile > click Create
- In the storage usage report wizard > type in a name for the report and tick Duplicate file candidates.
- Define the frequency and tick generate now.
- Click on the report task, and then click view complete report
- Finally, you should be in the Disk Usage Report|Duplicate File Candidates report a csv download link should be displayed on the right.

# Installation

Git clone on your Dsm, and build the jar with maven in /root
Inside the Dsm run the reporting and download the csv in the location of this jar.

Requirements
-----------------------------
- Synology DSM Nas (at least v.5.x)
- Java 8 installed on it
- Have duplicate file reporting enabled like here: https://www.youtube.com/watch?v=nWw4wo61v80

Features 
-----------------------------
- Duplicate files remover
- Test run possible with -dry_run option
- Compatible with Synology DSM 213j+

Screenshots
----------------------------

Usage
----------------------------
java -jar SynoFileDuplicateRemover.jar -csv_file [file.csv] -dry_run

-csv_file: (mandatory) name of csv containing the list of duplicate files generated by the Nas.

-dry_run: (optional) no argument, test run no deletion.

Post-installation
----------------------------
Configure cron task

TODO
----------------------------
- [ ] Tests with French characters
- [ ] Add csv file
- [ ] Add screenshots

