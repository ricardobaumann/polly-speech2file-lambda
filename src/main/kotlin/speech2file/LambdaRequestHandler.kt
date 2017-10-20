package speech2file


import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.polly.AmazonPollyClientBuilder
import com.amazonaws.services.polly.model.*
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.io.OutputStream

data class HandlerInput(val ssml: String, val bucketName: String = "bucket", val fileName: String = "file", val region: String = "eu-west-1", val publicFile: Boolean = true)
data class HandlerOutput(val status: String)

class LambdaRequestHandler : RequestStreamHandler {
    private val mapper = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val polly by lazy {
        AmazonPollyClientBuilder.defaultClient()
    }

    private val voice by lazy {
        polly.describeVoices(DescribeVoicesRequest().withLanguageCode(LanguageCode.DeDE)).voices[0]
    }

    override fun handleRequest(input: InputStream?, output: OutputStream?, context: Context?) {

        val inputObj = mapper.readValue<HandlerInput>(input!!)

        val amazons3 = AmazonS3ClientBuilder.standard().withRegion(inputObj.region).build()
        val transferManager = TransferManagerBuilder.standard().withS3Client(amazons3).build()

        val synthRequest = SynthesizeSpeechRequest()
                .withText(inputObj.ssml)
                .withTextType(TextType.Ssml)
                .withVoiceId(voice.id)
                .withOutputFormat(OutputFormat.Mp3)

        val pollyResult = polly.synthesizeSpeech(synthRequest).audioStream

        val objectMetadata = ObjectMetadata()
        val putObjectRequest = PutObjectRequest(inputObj.bucketName, inputObj.fileName, pollyResult, objectMetadata)
        if (inputObj.publicFile) {
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead)
        }
        val upload = transferManager.upload(putObjectRequest)
        upload.waitForUploadResult()

        mapper.writeValue(output, HandlerOutput(upload.state.toString()))
    }

}
