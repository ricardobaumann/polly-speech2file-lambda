package speech2file


import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.polly.AmazonPollyClientBuilder
import com.amazonaws.services.polly.model.DescribeVoicesRequest
import com.amazonaws.services.polly.model.OutputFormat
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest
import com.amazonaws.services.polly.model.TextType
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.io.OutputStream

data class HandlerInput(val ssml: String, val bucketName: String, val fileName: String)
data class HandlerOutput(val status: String)

class LambdaRequestHandler : RequestStreamHandler {
    private val mapper = jacksonObjectMapper()

    private val polly by lazy {
        AmazonPollyClientBuilder.defaultClient()
    }

    private val voice by lazy {
        polly.describeVoices(DescribeVoicesRequest()).voices[0]
    }

    private val amazons3 by lazy {
        AmazonS3ClientBuilder.defaultClient()
    }

    private val transferManager by lazy {
        TransferManagerBuilder.standard().withS3Client(amazons3).build()
    }

    override fun handleRequest(input: InputStream?, output: OutputStream?, context: Context?) {
        val inputObj = mapper.readValue<HandlerInput>(input!!)
        val synthRequest = SynthesizeSpeechRequest()
                .withText(inputObj.ssml)
                .withTextType(TextType.Ssml)
                .withVoiceId(voice.id)
                .withOutputFormat(OutputFormat.Mp3)

        val pollyResult = polly.synthesizeSpeech(synthRequest).audioStream

        val objectMetadata = ObjectMetadata()
        val upload = transferManager.upload(PutObjectRequest(inputObj.bucketName, inputObj.fileName, pollyResult, objectMetadata))
        upload.waitForUploadResult()

        mapper.writeValue(output, HandlerOutput(upload.state.toString()))
    }

}
