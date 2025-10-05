package com.example.veato

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TeamDetail : AppCompatActivity() {

    private lateinit var tvTeamName: TextView
    private lateinit var memberContainer: LinearLayout
    private lateinit var btnStartSession: Button

    private var team: Team? = null  // 🔹 전달받은 팀 데이터 저장용

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_detail)

        // View 연결
        tvTeamName = findViewById(R.id.tvTeamNameDetail)
        memberContainer = findViewById(R.id.memberContainer)
        btnStartSession = findViewById(R.id.btnStartSession)

        // 데이터 로드 → UI 반영
        loadData()
        setUI()

        // 버튼 클릭
        btnStartSession.setOnClickListener {
            team?.let {
                Toast.makeText(this, "${it.teamName} 세션 시작!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 전달받은 팀 객체 불러오기
    private fun loadData() {
        team = intent.getSerializableExtra("team") as? Team
        if (team == null) {
            Toast.makeText(this, "팀 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // 🔹 UI에 데이터 표시
    private fun setUI() {
        val currentTeam = team ?: return

        // 팀 이름 표시
        tvTeamName.text = currentTeam.teamName

        // 멤버 목록 표시
        memberContainer.removeAllViews()
        for (member in currentTeam.memberIDs) {
            val tv = TextView(this)
            tv.text = "• $member"
            tv.textSize = 16f
            tv.setPadding(8, 8, 8, 8)
            memberContainer.addView(tv)
        }
    }
}
