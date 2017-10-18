# Polly Speech to file lambda example

Based on a ssml text input, generates a mp3 file and saves it on a s3 bucket. 

## Usage
- Deploy it using `./gradlew clean build && serverless deploy`
- Test it using 

  `{
    "ssml": "<speak xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" version=\"1.0\"><metadata><dc:title xml:lang=\"en\">Telephone Menu: Level 1</dc:title></metadata><p><s xml:lang=\"en-US\">For English, press <emphasis>one</emphasis>.</s></p></speak>"
  `}
  
as input json
- Check it out on your s3 bucket for the generated mp3 file.
