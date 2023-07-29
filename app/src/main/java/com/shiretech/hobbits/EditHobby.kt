package com.shiretech.hobbits

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.min

class EditHobby : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_hobby)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""

        val hobbitEditTextMap = HashMap<Int, Array<EditText>>()

        hobbitEditTextMap[1] = arrayOf(
            findViewById(R.id.hobbits1_1),
            findViewById(R.id.hobbits1_2),
            findViewById(R.id.hobbits1_3)
        )

        hobbitEditTextMap[2] = arrayOf(
            findViewById(R.id.hobbits2_1),
            findViewById(R.id.hobbits2_2),
            findViewById(R.id.hobbits2_3)
        )

        hobbitEditTextMap[3] = arrayOf(
            findViewById(R.id.hobbits3_1),
            findViewById(R.id.hobbits3_2),
            findViewById(R.id.hobbits3_3)
        )

        val ButtonSetSchedule = findViewById<Button>(R.id.ButtonSetSched)
        ButtonSetSchedule.setOnClickListener {
            val hobbyName = intent.getStringExtra("hobbyName")
            val hobbits = intent.getStringArrayListExtra("hobbits")

            // Update the database with the edited hobbit data
            updateUserHobbyData(userId, hobbyName.toString(), hobbits, hobbitEditTextMap)

            // Start the activity for schedule_setup.xml
            val intent = Intent(this, SetUpSchedule::class.java)
            startActivity(intent)
        }

        val hobbyName = intent.getStringExtra("hobbyName")
        val hobbits = intent.getStringArrayListExtra("hobbits")

        val hobbyNameTextView = findViewById<TextView>(R.id.ChangeableHobbyName)
        val hobbit1EditText = findViewById<EditText>(R.id.FirstHobbits)
        val hobbit2EditText = findViewById<EditText>(R.id.SecondHobbits)
        val hobbit3EditText = findViewById<EditText>(R.id.ThirdHobbits)

        hobbyNameTextView.text = hobbyName

        database = FirebaseDatabase.getInstance().reference.child("categories")

        if (hobbits != null) {
            if (hobbits.size >= 1) {
                hobbit1EditText.setText("1. " + hobbits[0])
                fetchHobbitDataFromDatabase(hobbyName.toString(), hobbits[0], getHobbitEditText(1))
            }
            if (hobbits.size >= 2) {
                hobbit2EditText.setText("2. " + hobbits[1])
                fetchHobbitDataFromDatabase(hobbyName.toString(), hobbits[1], getHobbitEditText(2))
            }
            if (hobbits.size >= 3) {
                hobbit3EditText.setText("3. " + hobbits[2])
                fetchHobbitDataFromDatabase(hobbyName.toString(), hobbits[2], getHobbitEditText(3))
            }
        }
    }
    private fun getHobbitEditText(hobbitIndex: Int): Array<EditText> {
        return when (hobbitIndex) {
            1 -> arrayOf(
                findViewById(R.id.hobbits1_1),
                findViewById(R.id.hobbits1_2),
                findViewById(R.id.hobbits1_3)
            )

            2 -> arrayOf(
                findViewById(R.id.hobbits2_1),
                findViewById(R.id.hobbits2_2),
                findViewById(R.id.hobbits2_3)
            )

            3 -> arrayOf(
                findViewById(R.id.hobbits3_1),
                findViewById(R.id.hobbits3_2),
                findViewById(R.id.hobbits3_3)
            )

            else -> emptyArray()
        }
    }
    private fun fetchHobbitDataFromDatabase(
        hobbyName: String,
        hobbitName: String,
        hobbitEditTexts: Array<EditText>
    ) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val categories = dataSnapshot.children
                for (categorySnapshot in categories) {
                    val hobbies = categorySnapshot.child("hobbies").children
                    for (hobbySnapshot in hobbies) {
                        val name = hobbySnapshot.child("name").value as String
                        if (name == hobbyName) {
                            val hobbits = hobbySnapshot.child("hobbits").children
                            for (hobbitSnapshot in hobbits) {
                                val hobbit = hobbitSnapshot.child("name").value as String
                                if (hobbit == hobbitName) {
                                    // Assuming the data you want to set is in 'bits' node
                                    val bits = hobbitSnapshot.child("bits").value as List<String>?
                                    if (bits != null && bits.isNotEmpty()) {
                                        for (i in 0 until min(hobbitEditTexts.size, bits.size)) {
                                            val bitNumber = i + 1
                                            val bitWithNumber = "$bitNumber. ${bits[i]}"
                                            val editText = hobbitEditTexts[i]
                                            editText.setText(bitWithNumber)
                                        }
                                        // Clear remaining EditTexts if there are fewer bits than EditTexts
                                        for (i in bits.size until hobbitEditTexts.size) {
                                            hobbitEditTexts[i].setText("")
                                        }
                                    } else {
                                        // Clear all EditTexts if no bits available
                                        for (editText in hobbitEditTexts) {
                                            editText.setText("")
                                        }
                                    }
                                    break
                                }
                            }
                            break
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("EditHobby", "Failed to fetch hobbit data: ${databaseError.message}")
            }
        })
    }
    private fun updateUserHobbyData(userId: String, hobbyName: String, hobbits: ArrayList<String>?, hobbitEditTextMap: HashMap<Int, Array<EditText>>) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val userRef = usersRef.child(userId)

        // Create the "categories" node under the user's node
        val categoriesRef = userRef.child("categories")

        // Fetch the current number of hobbies to determine the next index
        val hobbiesRef = categoriesRef.child("hobbies")
        hobbiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hobbyIndex = dataSnapshot.childrenCount.toInt()

                // Create the "hobbies" node under the "categories" node
                val hobbyRef = hobbiesRef.child("savedhobby$hobbyIndex")
                hobbyRef.child("name").setValue(hobbyName)

                // Create the "hobbits" node under the "hobbies" node
                val hobbitsRef = hobbyRef.child("hobbits")
                for ((index, hobbit) in hobbits!!.withIndex()) {
                    val hobbitRef = hobbitsRef.child("hobbit$index")
                    hobbitRef.child("name").setValue(hobbit)

                    val hobbitEditTexts = hobbitEditTextMap[index + 1]
                    if (hobbitEditTexts != null) {
                        val bitsRef = hobbitRef.child("bits")
                        val bitTextList = hobbitEditTexts.map { it.text.toString().trim() }.filter { it.isNotEmpty() }
                        for ((bitIndex, bitText) in bitTextList.withIndex()) {
                            bitsRef.child("bit$bitIndex").setValue(bitText)
                        }
                    }
                }

                Log.d("EditHobby", "Hobbits updated in the database")
                Toast.makeText(applicationContext, "Hobbits updated successfully!", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("EditHobby", "Failed to fetch hobbies: ${databaseError.message}")
            }
        })
    }
}