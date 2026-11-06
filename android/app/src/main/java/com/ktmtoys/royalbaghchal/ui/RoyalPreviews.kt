package com.ktmtoys.royalbaghchal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktmtoys.royalbaghchal.game.Rules

@Preview(name="Android · Royal Game",showBackground=true,widthDp=393,heightDp=852)
@Composable
fun RoyalBoardPreview(){
    RoyalTheme {
        RoyalBackdrop(Modifier.fillMaxSize()){
            Column(Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(14.dp)){
                CrownEmblem(size=86.dp)
                Text("बाघचाल",fontSize=38.sp,fontWeight=FontWeight.Bold,color=Royal.GoldLight)
                RoyalPanel(Modifier.fillMaxWidth()){
                    MicroLabel("THE ROYAL ARENA · NEPAL")
                    Spacer(Modifier.height(12.dp))
                    val game=Rules.initial()
                    RoyalBoard(game,null,null,Rules.legalMoves(game),true,"ne"){}
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        StatPill("TIGERS","04",Modifier.weight(1f))
                        StatPill("GOATS","20",Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(name="Android · Royal Crest",showBackground=true,widthDp=393,heightDp=365)
@Composable
fun RoyalIdentityPreview(){
    RoyalTheme {
        RoyalBackdrop(Modifier.fillMaxSize()){
            RoyalPanel(Modifier.fillMaxWidth().padding(18.dp),padding=24.dp){
                CrownEmblem(Modifier.align(Alignment.CenterHorizontally),size=110.dp)
                Spacer(Modifier.height(5.dp))
                Text("KTM TOYS NEPAL",fontSize=18.sp,fontWeight=FontWeight.Bold,
                    color=Royal.GoldLight,modifier=Modifier.align(Alignment.CenterHorizontally))
                MicroLabel("ॐ · CHANDRA — SURYA · HIMALAYA",
                    Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(14.dp))
                GoldButton("ENTER THE COURT",{},Modifier.fillMaxWidth())
            }
        }
    }
}
