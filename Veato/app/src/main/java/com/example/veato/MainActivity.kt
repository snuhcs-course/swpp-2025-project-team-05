package com.example.veato

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {


    private lateinit var teamContainer: LinearLayout
    private lateinit var btnCreateTeam: Button

    // 팀 데이터 리스트 (임시)
    private val teamList = mutableListOf<Team>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // View 연결
        teamContainer = findViewById(R.id.teamContainer)
        btnCreateTeam = findViewById(R.id.btnCreateTeam)

        // 데이터 불러오기 및 UI 표시
        loadData()
        setUI()

        // 새 팀 만들기 버튼
        btnCreateTeam.setOnClickListener {
            Toast.makeText(this, "새 팀 만들기 클릭됨", Toast.LENGTH_SHORT).show()
        }


    }






    // 데이터 불러오기 (임시 하드코딩 — 나중에 DB 연결 가능)
    private fun loadData() {
        teamList.clear()

        teamList.add(Team("Team Alpha", "kim", listOf("kim", "lee", "park")))
        teamList.add(Team("Team Bravo", "lee", listOf("kim", "lee", "park", "choi")))
        teamList.add(Team("Team Gamma", "park", listOf("kim", "lee")))
    }

    // 화면에 팀 목록 표시
    private fun setUI() {
        val inflater = LayoutInflater.from(this)
        teamContainer.removeAllViews() // 중복 방지

        for (team in teamList) {
            // item_team.xml 불러오기
            val teamView = inflater.inflate(R.layout.item_team, teamContainer, false)

            // 내부 요소 찾기
            val tvName = teamView.findViewById<TextView>(R.id.tvTeamName)
            val tvCount = teamView.findViewById<TextView>(R.id.tvMemberCount)
            val btnEnter = teamView.findViewById<Button>(R.id.btnEnterTeam)

            // 데이터 적용
            tvName.text = team.teamName
            tvCount.text = "${team.memberIDs.size} members"

            // 버튼 클릭 시
            btnEnter.setOnClickListener {
                val intent = Intent(this, TeamDetail::class.java)
                intent.putExtra("team", team)  // ✅ Team 객체 전달
                startActivity(intent)
            }

            // 최종 추가
            teamContainer.addView(teamView)
        }
    }
}