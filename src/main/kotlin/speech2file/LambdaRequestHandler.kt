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

data class HandlerInput(val ssml: String)
data class HandlerOutput(val message: String)

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
        //objectMetadata.contentLength = pollyResult.
        val upload = transferManager.upload(PutObjectRequest("polly-test2", "polly.mp3", pollyResult, objectMetadata))
        upload.waitForUploadResult()

        mapper.writeValue(output, HandlerOutput(upload.state.toString()))
    }

    val s = "<speak xmlns=\"http://www.w3.org/2001/10/synthesis\"\n" +
            "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
            "       version=\"1.0\">\n" +
            "  <metadata>\n" +
            "    <dc:title xml:lang=\"en\">Telephone Menu: Level 1</dc:title>\n" +
            "  </metadata>\n" +
            "\n" +
            "  <p>\n" +
            "    <s xml:lang=\"en-US\">\n" +
            "      <voice name=\"David\" gender=\"male\" age=\"25\">\n" +
            "        For English, press <emphasis>one</emphasis>.\n" +
            "      </voice>\n" +
            "    </s>\n" +
            "    <s xml:lang=\"es-MX\">\n" +
            "      <voice name=\"Miguel\" gender=\"male\" age=\"25\">\n" +
            "        Para español, oprima el <emphasis>dos</emphasis>.\n" +
            "      </voice>\n" +
            "    </s>\n" +
            "  </p>\n" +
            "\n" +
            "</speak>"

}
