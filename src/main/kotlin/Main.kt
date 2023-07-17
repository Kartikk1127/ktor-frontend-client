import com.jsoniter.output.JsonStream
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtils
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Serializable
data class Matrix(val rows: Int, val cols: Int, val values: List<List<Int>>)

@Serializable
data class MatrixMultiplicationRequest(
    val matrix1: Matrix,
    val matrix2: Matrix
)

suspend fun main() {
    val beginningTime = System.currentTimeMillis()
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    val responseList = mutableListOf<Deferred<HttpResponse>>()
    val successfullRequestList = mutableListOf<HttpResponse>()
    val latencyList = mutableListOf<Long>()
    var beginTime=0L
    var endingTime=0L

    val totalTime = 30
    val totalRequests = 180000
    val requestsPerSecond = totalRequests / totalTime

    var requestCount = 0
    val startTime = System.currentTimeMillis()
    var endTime = startTime + totalTime * 1000

    val dataset = DefaultCategoryDataset()
//    var latencyListCopy = mutableListOf<Long>()

    while (System.currentTimeMillis() < endTime && requestCount < totalRequests) {
        val remainingRequests = totalRequests - requestCount
        val requestsToSend = minOf(requestsPerSecond, remainingRequests)
        var response : Deferred<HttpResponse> = CoroutineScope(Dispatchers.IO).async { TODO() }

        repeat(requestsToSend) {
            val matrix1 = generateRandomMatrix(2, 2)
            val matrix2 = generateRandomMatrix(2, 2)
            response = CoroutineScope(Dispatchers.IO).async {
//                beginTime = System.currentTimeMillis()
                val result = postMatrixMultiplication(client, matrix1, matrix2, latencyList, successfullRequestList, dataset, beginningTime)
//                endingTime = System.currentTimeMillis()
                result
            }

            requestCount++
        }
//        val currentRequests = requestCount
        delay(1000)
        if(response.isCompleted){
            responseList.add(response)
        }
        else
        {
            response.cancel()
        }
    }
    val chart: JFreeChart = ChartFactory.createBarChart(
        "Latency Graph",
        "Time",
        "Latency(ms)",
        dataset,
        PlotOrientation.VERTICAL,
        true,
        true,
        false
    )

    val output = File("chart.png")
    ChartUtils.saveChartAsPNG(output,chart,800,600)

    val sortedLatencyList = latencyList.sorted()
    val p99Index = (sortedLatencyList.size * 0.99).toInt()
    val p95Index = (sortedLatencyList.size * 0.95).toInt()
    val p50Index = (sortedLatencyList.size * 0.50).toInt()

    val p99 = sortedLatencyList[p99Index]
    val p95 = sortedLatencyList[p95Index]
    val p50 = sortedLatencyList[p50Index]

    println("Total requests sent: $requestCount")
    println("Minimum response time: ${latencyList.min()} ms")
    println("Maximum response time: ${latencyList.max()} ms")
//    println("Average response time: ${latencyList.average()} ms")
    println("Latency list: $latencyList")
    println("Successful requests: ${successfullRequestList.size}")
    println("p99 is : $p99")
    println("p95 is : $p95")
    println("p50 is : $p50")



    client.close()
}

suspend fun postMatrixMultiplication(
    client: HttpClient,
    matrix1: Matrix,
    matrix2: Matrix,
    latency: MutableList<Long>,
    successfullList: MutableList<HttpResponse>,
    dataset: DefaultCategoryDataset,
    beginTime: Long
): HttpResponse {
    val request = MatrixMultiplicationRequest(matrix1, matrix2)
    val json = JsonStream.serialize(request)
    val response: HttpResponse
//    println(json)
    val timeTaken = measureTimeMillis { response= client.post("http://localhost:8080/api/matrix-multiply") {
        contentType(ContentType.Application.Json)
        setBody(json)
    } }

    if (response.status == HttpStatusCode.OK) {
        successfullList.add(response)
//        val currentLatency = System.currentTimeMillis()
        latency.add(timeTaken)
        dataset.addValue(timeTaken, "Latency", timeTaken) // Add latency value to the dataset
    } else {
        println(response.status)
    }
    // println(response.bodyAsText())

    return response
}

fun generateRandomMatrix(rows: Int, columns: Int): Matrix {
    val values = List(rows) {
        List(columns) {
            Random.nextInt(1, 10) // Generate random values between 1 and 10
        }
    }
    return Matrix(rows, columns, values)
}
