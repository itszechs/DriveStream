package zechs.drive.stream.ui.player2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import zechs.drive.stream.databinding.ActivityMpvBinding


class MPVActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMpvBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMpvBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}