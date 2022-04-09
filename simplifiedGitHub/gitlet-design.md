# Gitlet Design Document

**Name**: Xiaowen Liu

## Classes and Data Structures

### Commit

#### Object's Instance variables

1. String Message - message said by the user
2. String Timestamp - time at which a commit was created
3. String Parent id - sha1 code of the parent commit of the commit
4. HashMap - tracing the file name & corresponding blob id

   


### Repository

#### Initial's function

1. Create the .gitlet dictionary
2. Create the staging area (addition & removal)
3. Create the commits area

### Blob - File
#### Variables
1. File path
2. SHA 1 code of its own


## Algorithms
### Add

#### Steps/Structure
General
1. Find the address of the file by join(CWD, ...)
2. Read the content and collect as strings
3. Find the sha1 - ID of the content
----
Stages
4. Compare the content & commit's version
   - if same, don't add to stages
   - else: add to stages
5. Create a folder called stages (in initial?)
6. Write the content into a file with name = sha-ID
7. QUESTIONS:
   - HOW TO FIND THE CURRENT COMMIT
   - WHAT THE SPEC MEANS BY DIFFERENTIATING BLOB'S SHA-ID & COMMIT'S BY ADDING EXTRA WORD
   
----
Blob
8. Create a dictionary as blobs (in initial?)
9. Write the content into a file with name = sha - ID
10. QUESTIONS:
    - DO WE NEED REFERENCE TO BLOBS IN STAGING FILES?





## Persistence

### Head commit

