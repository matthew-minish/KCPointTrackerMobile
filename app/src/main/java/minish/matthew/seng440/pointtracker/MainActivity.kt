package minish.matthew.seng440.pointtracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.activityUiThreadWithContext
import org.jetbrains.anko.doAsync
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Collectors


data class NTuple4<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, var t4: T4)

class MainActivity : AppCompatActivity() {

    private lateinit var blueBoysDisplayText: TextView
    private lateinit var greenBoysDisplayText: TextView
    private lateinit var orangeBoysDisplayText: TextView
    private lateinit var redBoysDisplayText: TextView
    private lateinit var yellowBoysDisplayText: TextView
    private lateinit var blueGirlsDisplayText: TextView
    private lateinit var greenGirlsDisplayText: TextView
    private lateinit var orangeGirlsDisplayText: TextView
    private lateinit var redGirlsDisplayText: TextView
    private lateinit var yellowGirlsDisplayText: TextView

    private lateinit var blueBoysPlusTen: Button
    private lateinit var greenBoysPlusTen: Button
    private lateinit var orangeBoysPlusTen: Button
    private lateinit var redBoysPlusTen: Button
    private lateinit var yellowBoysPlusTen: Button
    private lateinit var blueGirlsPlusTen: Button
    private lateinit var greenGirlsPlusTen: Button
    private lateinit var orangeGirlsPlusTen: Button
    private lateinit var redGirlsPlusTen: Button
    private lateinit var yellowGirlsPlusTen: Button

    private lateinit var blueBoysMinusTen: Button
    private lateinit var greenBoysMinusTen: Button
    private lateinit var orangeBoysMinusTen: Button
    private lateinit var redBoysMinusTen: Button
    private lateinit var yellowBoysMinusTen: Button
    private lateinit var blueGirlsMinusTen: Button
    private lateinit var greenGirlsMinusTen: Button
    private lateinit var orangeGirlsMinusTen: Button
    private lateinit var redGirlsMinusTen: Button
    private lateinit var yellowGirlsMinusTen: Button

    private lateinit var syncWithServerButton: Button
    private lateinit var serverAddressEditText: EditText

    private lateinit var checkHealthButton: Button

    private lateinit var teamsDisplayPlusMinusPoints: List<NTuple4<TextView, Button, Button, Int>>

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        assignViews()

        sharedPreferences = getSharedPreferences("KidsCampPointsTracker", Context.MODE_PRIVATE)

        teamsDisplayPlusMinusPoints = listOf(
            NTuple4(blueBoysDisplayText, blueBoysPlusTen, blueBoysMinusTen, 0),
            NTuple4(greenBoysDisplayText, greenBoysPlusTen, greenBoysMinusTen, 0),
            NTuple4(orangeBoysDisplayText, orangeBoysPlusTen, orangeBoysMinusTen, 0),
            NTuple4(redBoysDisplayText, redBoysPlusTen, redBoysMinusTen, 0),
            NTuple4(yellowBoysDisplayText, yellowBoysPlusTen, yellowBoysMinusTen, 0),
            NTuple4(blueGirlsDisplayText, blueGirlsPlusTen, blueGirlsMinusTen, 0),
            NTuple4(greenGirlsDisplayText, greenGirlsPlusTen, greenGirlsMinusTen, 0),
            NTuple4(orangeGirlsDisplayText, orangeGirlsPlusTen, orangeGirlsMinusTen, 0),
            NTuple4(redGirlsDisplayText, redGirlsPlusTen, redGirlsMinusTen, 0),
            NTuple4(yellowGirlsDisplayText, yellowGirlsPlusTen, yellowGirlsMinusTen, 0)
        )

        if(sharedPreferences.contains("savedPoints")) {
            try {
                val itemType = object : TypeToken<List<Int>>() {}.type
                val points: List<Int> = Gson().fromJson(sharedPreferences.getString("savedPoints", null), itemType)
                for (x in 0..9) {
                    teamsDisplayPlusMinusPoints[x].t4 = points[x]
                }
            } catch(e: Exception) {
                Toast.makeText(this, "Error reading last points", Toast.LENGTH_LONG).show()
            }
        }

        if(sharedPreferences.contains("serverAddress")) {
            serverAddressEditText.setText(sharedPreferences.getString("serverAddress", ""));
        }

        assignHandlers()
    }

    private fun assignHandlers() {
        for (tuple: NTuple4<TextView, Button, Button, Int> in teamsDisplayPlusMinusPoints) {
            tuple.t1.text = tuple.t4.toString()

            tuple.t2.setOnClickListener {
                tuple.t4 += 10
                tuple.t1.text = tuple.t4.toString()
            }

            tuple.t3.setOnClickListener {
                tuple.t4 -= 10
                tuple.t1.text = tuple.t4.toString()
            }
        }

        syncWithServerButton.setOnClickListener {
            sendNetworkPoints()
        }

        checkHealthButton.setOnClickListener {
            checkAPIHealth()
        }
    }

    private fun refreshPointLabels() {
        for (tuple: NTuple4<TextView, Button, Button, Int> in teamsDisplayPlusMinusPoints) {
            tuple.t1.text = tuple.t4.toString()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val myEdit = sharedPreferences.edit()
        val pointsList: List<Int> = teamsDisplayPlusMinusPoints.stream().map {x -> x.t4}.collect(Collectors.toList())
        val pointsString = Gson().toJson(pointsList)
        myEdit.putString("savedPoints", pointsString)
        myEdit.putString("serverAddress", serverAddressEditText.text.toString())
        myEdit.apply()
    }

    private fun sendNetworkPoints() {
        val pointsMap = mapOf(
            "Blue Boys" to teamsDisplayPlusMinusPoints[0].t4,
            "Green Boys" to teamsDisplayPlusMinusPoints[1].t4,
            "Orange Boys" to teamsDisplayPlusMinusPoints[2].t4,
            "Red Boys" to teamsDisplayPlusMinusPoints[3].t4,
            "Yellow Boys" to teamsDisplayPlusMinusPoints[4].t4,
            "Blue Girls" to teamsDisplayPlusMinusPoints[5].t4,
            "Green Girls" to teamsDisplayPlusMinusPoints[6].t4,
            "Orange Girls" to teamsDisplayPlusMinusPoints[7].t4,
            "Red Girls" to teamsDisplayPlusMinusPoints[8].t4,
            "Yellow Girls" to teamsDisplayPlusMinusPoints[9].t4
        )

        val requestBody: String = Gson().toJson(mapOf("pointChanges" to pointsMap))

        doAsync {
            try {
                val url = URL("http://" + serverAddressEditText.editableText + "/changepoints")
                url.openConnection()
                    .let {
                        it as HttpURLConnection
                    }.apply {
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        requestMethod = "POST"
                        doOutput = true
                        val outputWriter = OutputStreamWriter(outputStream)
                        outputWriter.write(requestBody)
                        outputWriter.flush()
                    }.apply {
                        if (this.responseCode == 200) {
                            teamsDisplayPlusMinusPoints[0].t4 = 0
                            teamsDisplayPlusMinusPoints[1].t4 = 0
                            teamsDisplayPlusMinusPoints[2].t4 = 0
                            teamsDisplayPlusMinusPoints[3].t4 = 0
                            teamsDisplayPlusMinusPoints[4].t4 = 0
                            teamsDisplayPlusMinusPoints[5].t4 = 0
                            teamsDisplayPlusMinusPoints[6].t4 = 0
                            teamsDisplayPlusMinusPoints[7].t4 = 0
                            teamsDisplayPlusMinusPoints[8].t4 = 0
                            teamsDisplayPlusMinusPoints[9].t4 = 0

                            refreshPointLabels()
                        } else {
                            val statusCode = this.responseCode
                            activityUiThreadWithContext {
                                Toast.makeText(
                                    this,
                                    "Error code received: $statusCode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
            } catch (e: Exception) {
                activityUiThreadWithContext {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAPIHealth() {
        val url = URL("http://" + serverAddressEditText.editableText + "/healthcheck")
        doAsync {
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"

            try {
                if (con.responseCode == 200) {
                    activityUiThreadWithContext {
                        Toast.makeText(
                            this,
                            "Success! Ready to sync...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    activityUiThreadWithContext {
                        Toast.makeText(
                            this,
                            "Error! Bad response code: " + con.responseCode.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                activityUiThreadWithContext {
                    Toast.makeText(
                        this,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun assignViews() {
        blueBoysDisplayText = findViewById(R.id.blueBoysPointsDisplay)
        greenBoysDisplayText = findViewById(R.id.greenBoysPointsDisplay)
        orangeBoysDisplayText = findViewById(R.id.orangeBoysPointsDisplay)
        redBoysDisplayText = findViewById(R.id.redBoysPointsDisplay)
        yellowBoysDisplayText = findViewById(R.id.yellowBoysPointsDisplay)
        blueGirlsDisplayText = findViewById(R.id.blueGirlsPointsDisplay)
        greenGirlsDisplayText = findViewById(R.id.greenGirlsPointsDisplay)
        orangeGirlsDisplayText = findViewById(R.id.orangeGirlsPointsDisplay)
        redGirlsDisplayText = findViewById(R.id.redGirlsPointsDisplay)
        yellowGirlsDisplayText = findViewById(R.id.yellowGirlsPointsDisplay)

        blueBoysPlusTen = findViewById(R.id.blueBoysPlusTen)
        greenBoysPlusTen = findViewById(R.id.greenBoysPlusTen)
        orangeBoysPlusTen = findViewById(R.id.orangeBoysPlusTen)
        redBoysPlusTen = findViewById(R.id.redBoysPlusTen)
        yellowBoysPlusTen = findViewById(R.id.yellowBoysPlusTen)
        blueGirlsPlusTen = findViewById(R.id.blueGirlsPlusTen)
        greenGirlsPlusTen = findViewById(R.id.greenGirlsPlusTen)
        orangeGirlsPlusTen = findViewById(R.id.orangeGirlsPlusTen)
        redGirlsPlusTen = findViewById(R.id.redGirlsPlusTen)
        yellowGirlsPlusTen = findViewById(R.id.yellowGirlsPlusTen)

        blueBoysMinusTen = findViewById(R.id.blueBoysMinusTen)
        greenBoysMinusTen = findViewById(R.id.greenBoysMinusTen)
        orangeBoysMinusTen = findViewById(R.id.orangeBoysMinusTen)
        redBoysMinusTen = findViewById(R.id.redBoysMinusTen)
        yellowBoysMinusTen = findViewById(R.id.yellowBoysMinusTen)
        blueGirlsMinusTen = findViewById(R.id.blueGirlsMinusTen)
        greenGirlsMinusTen = findViewById(R.id.greenGirlsMinusTen)
        orangeGirlsMinusTen = findViewById(R.id.orangeGirlsMinusTen)
        redGirlsMinusTen = findViewById(R.id.redGirlsMinusTen)
        yellowGirlsMinusTen = findViewById(R.id.yellowGirlsMinusTen)

        syncWithServerButton = findViewById(R.id.syncButton)
        serverAddressEditText = findViewById(R.id.serverAddressEditText)

        checkHealthButton = findViewById(R.id.checkHealthButton)
    }
}
