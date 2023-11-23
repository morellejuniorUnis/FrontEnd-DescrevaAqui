package com.example.descrevaaqui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.descrevaaqui.ui.theme.DescrevaAquiTheme
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import com.google.gson.Gson


interface ApiService {
    @POST("main/transcription")
    fun sendImage(@Header("Authorization") token: String, @Body requestBody: RequestBody): Call<ApiResponse>
}

data class ApiResponse(
    @SerializedName("textTranscreved") val textTranscreved: String?
)

class MainActivity : ComponentActivity() {

    private var imageDataBase64 by mutableStateOf<String?>(null)
    private var apiResponse by mutableStateOf<ApiResponse?>(null)
    private var isLoading by mutableStateOf(false)

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                val byteArrayOutputStream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                imageDataBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
                sendImageToApi(imageDataBase64)
            }
        }

    private fun onTakePhoto() {
        takePhoto.launch(null)
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://faculapi.avlq.online/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DescrevaAquiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFFFFF.toInt()),
                ) {
                    Greeting("Android", apiResponse?.textTranscreved, this@MainActivity::onImageSelected)
                }
            }
        }
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val byteArrayOutputStream = ByteArrayOutputStream()

                try {
                    inputStream?.copyTo(byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    imageDataBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)

                    sendImageToApi(imageDataBase64)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    inputStream?.close()
                    byteArrayOutputStream.close()
                }
            }
        }

    private fun onImageSelected() {
        getContent.launch("image/*")
    }

    private fun sendImageToApi(imageDataBase64: String?) {
        if (!imageDataBase64.isNullOrEmpty()) {
            Log.d("API", "Preparando para enviar a imagem para a API")

            val jsonBody = mapOf("imageDataBase64" to imageDataBase64)

            val gson = Gson()
            val requestBody = gson.toJson(jsonBody).toRequestBody("application/json".toMediaTypeOrNull())
            val token = "9324NV032-5V2222vl2c"

            val url = "https://faculapi.avlq.online/main/transcription"
            Log.d("API", "URL: $url")

            val call = apiService.sendImage(token, requestBody)

            isLoading = true

            call.enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    Log.d("API", "Código de resposta: ${response.code()}")
                    Log.d("API", "Cabeçalhos da resposta: ${response.headers()}")
                    val responseBodyString = response.body().toString()
                    Log.d("API", "Corpo da resposta: $responseBodyString")

                    if (response.isSuccessful) {
                        apiResponse = response.body()

                        if (apiResponse != null) {
                            val transcription = apiResponse!!.textTranscreved

                            Log.d("API", "Transcrição: $transcription")

                            apiResponse = ApiResponse(textTranscreved = transcription)
                        } else {
                            Log.e("API", "O corpo da resposta é nulo ou não contém os campos esperados.")
                        }
                    } else {
                        Log.e("API", "Erro: ${response.code()}")
                    }

                    isLoading = false
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e("API", "Erro: ${t.message}")
                    isLoading = false
                }
            })
        }
    }

    @Composable
    fun Greeting(name: String, transcription: String?, onImageSelected: () -> Unit, modifier: Modifier = Modifier) {
        val buttonColor = Color(0xFF82B9E6.toInt())

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { onImageSelected() },
                    colors = ButtonDefaults.buttonColors(buttonColor),
                    modifier = Modifier
                ) {
                    Text(text = "Enviar Imagem")
                }

                Button(
                    onClick = { onTakePhoto() },
                    colors = ButtonDefaults.buttonColors(buttonColor),
                    modifier = Modifier
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_camera_alt_24_foreground),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(text = "Tirar Foto", modifier = Modifier.padding(start = 8.dp))
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(buttonColor)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = buttonColor
                    )
                }
            } else {
                Box {
                    transcription?.let {
                        OutlinedTextField(
                            value = it,
                            onValueChange = {},
                            label = { Text("Imagem-Convertida", style = TextStyle(color = buttonColor)) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp),
                        )
                    }
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        DescrevaAquiTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black,
            ) {
                Greeting("Android", null, {})
            }
        }
    }
}


