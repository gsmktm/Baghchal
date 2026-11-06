package com.ktmtoys.royalbaghchal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktmtoys.royalbaghchal.R
import com.ktmtoys.royalbaghchal.game.*
import kotlin.math.min

private val goldEdge=Brush.linearGradient(
    listOf(Royal.GoldDark,Royal.GoldLight,Royal.GoldDark))
@Composable
fun RoyalBackdrop(modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit){
    Box(modifier.background(Brush.verticalGradient(
        listOf(Royal.Navy,Royal.Navy2,Color(0xFF060B14))))) {
        Canvas(Modifier.fillMaxWidth().height(270.dp).align(Alignment.TopCenter)){
            val w=size.width;val h=size.height
            val back=Path().apply {
                moveTo(0f,h*.76f);lineTo(w*.12f,h*.49f);lineTo(w*.22f,h*.65f)
                lineTo(w*.43f,h*.19f);lineTo(w*.62f,h*.7f)
                lineTo(w*.76f,h*.4f);lineTo(w,h*.7f);lineTo(w,h);lineTo(0f,h);close()
            }
            drawPath(back,Color(0xFF35506D).copy(alpha=.28f))
            val fore=Path().apply {
                moveTo(0f,h*.84f);lineTo(w*.17f,h*.64f);lineTo(w*.28f,h*.76f)
                lineTo(w*.58f,h*.33f);lineTo(w*.76f,h*.79f)
                lineTo(w*.9f,h*.56f);lineTo(w,h*.73f);lineTo(w,h);lineTo(0f,h);close()
            }
            drawPath(fore,Color(0xFF13263C).copy(alpha=.88f))
            drawCircle(Royal.Gold.copy(alpha=.07f),radius=w*.23f,center=Offset(w*.5f,h*.39f))
        }
        content()
    }
}
@Composable
fun CrownEmblem(modifier:Modifier=Modifier,size:Dp=92.dp){
    Box(modifier.size(size),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w=this.size.width; val h=this.size.height
            val path=Path().apply {
                moveTo(w*.12f,h*.44f);lineTo(w*.24f,h*.53f)
                lineTo(w*.36f,h*.27f);lineTo(w*.5f,h*.53f)
                lineTo(w*.64f,h*.27f);lineTo(w*.76f,h*.53f)
                lineTo(w*.88f,h*.44f);lineTo(w*.78f,h*.78f)
                quadraticTo(w*.50f,h*.87f,w*.22f,h*.78f);close()
            }
            drawPath(path,Royal.Panel)
            drawPath(path,Royal.GoldLight,style=Stroke(width=3.dp.toPx()))
            drawLine(Royal.Crimson,Offset(w*.25f,h*.71f),Offset(w*.75f,h*.71f),
                strokeWidth=7.dp.toPx(),cap=StrokeCap.Round)
            val diamond=Path().apply {
                moveTo(w*.5f,h*.47f);lineTo(w*.57f,h*.59f)
                lineTo(w*.5f,h*.71f);lineTo(w*.43f,h*.59f);close()
            }
            drawPath(diamond,Royal.Crimson)
            drawPath(diamond,Royal.GoldLight,style=Stroke(2.dp.toPx()))
            drawCircle(Royal.Ivory,radius=3.dp.toPx(),center=Offset(w*.5f,h*.59f))
        }
        Text("ॐ",color=Royal.GoldLight,fontSize=(size.value*.25f).sp,
            fontWeight=FontWeight.Bold,modifier=Modifier.offset(y=-(size*.3f)))
    }
}
@Composable
fun RoyalPanel(modifier:Modifier=Modifier,padding:Dp=18.dp,content:@Composable ColumnScope.()->Unit){
    Column(modifier.clip(RoundedCornerShape(18.dp))
        .background(Brush.linearGradient(listOf(Royal.Panel,Royal.PanelDeep)))
        .border(1.dp,goldEdge,RoundedCornerShape(18.dp))
        .padding(padding),content=content)
}
@Composable
fun GoldButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,
    enabled:Boolean=true,primary:Boolean=true){
    val shape=RoundedCornerShape(12.dp)
    if(primary)Button(onClick=onClick,modifier=modifier.heightIn(min=51.dp),enabled=enabled,
        shape=shape,colors=ButtonDefaults.buttonColors(
            containerColor=Royal.GoldLight,contentColor=Royal.Navy,
            disabledContainerColor=Royal.GoldDark,disabledContentColor=Royal.Navy2
        )){
        Text(text,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
    }else OutlinedButton(onClick=onClick,modifier=modifier.heightIn(min=51.dp),
        enabled=enabled,shape=shape,border=androidx.compose.foundation.BorderStroke(1.dp,Royal.Gold.copy(alpha=.6f)),
        colors=ButtonDefaults.outlinedButtonColors(contentColor=Royal.GoldLight)){
        Text(text,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
    }
}
@Composable
fun MicroLabel(text:String,modifier:Modifier=Modifier){
    Text(text.uppercase(),modifier=modifier,color=Royal.Gold,
        style=androidx.compose.material3.MaterialTheme.typography.labelMedium,
        letterSpacing=1.8.sp)
}
@Composable
fun PieceToken(side:Side,modifier:Modifier=Modifier,size:Dp=44.dp,selected:Boolean=false){
    val scale by animateFloatAsState(if(selected)1.1f else 1f,label="Royal piece emphasis")
    val rim=if(side==Side.TIGER)Royal.GoldLight else Royal.Ivory
    val gradient=if(side==Side.TIGER)listOf(Color(0xFFE7B073),Royal.WoodDark)
        else listOf(Color.White,Color(0xFFC5B7A0))
    Box(modifier.size(size).scale(scale).clip(CircleShape)
        .background(Brush.radialGradient(gradient))
        .border(if(selected)3.dp else 2.dp,
            if(selected)Royal.Mint else rim,CircleShape),
        contentAlignment=Alignment.Center){
        Image(painterResource(if(side==Side.TIGER)R.drawable.ic_tiger else R.drawable.ic_goat),
            contentDescription=null,modifier=Modifier.fillMaxSize(.87f))
    }
}
@Composable
fun StatPill(title:String,value:String,modifier:Modifier=Modifier){
    Column(modifier.clip(RoundedCornerShape(14.dp))
        .background(Royal.PanelDeep.copy(alpha=.94f))
        .border(1.dp,Royal.GoldDark.copy(alpha=.55f),RoundedCornerShape(14.dp))
        .padding(horizontal=13.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Text(value,color=Royal.GoldLight,fontWeight=FontWeight.Bold,fontSize=23.sp)
        Text(title,color=Royal.Fog,fontSize=11.sp,textAlign=TextAlign.Center)
    }
}
@Composable
fun RoyalBoard(game:GameState,selected:Int?,hint:Move?,allowed:List<Move>,
    enabled:Boolean,language:String,onNode:(Int)->Unit){
    val haptic=LocalHapticFeedback.current
    BoxWithConstraints(
        modifier=Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(listOf(Royal.WoodLight,Royal.Wood,
                Royal.WoodDark,Royal.WoodLight)))
            .border(3.dp,Royal.GoldLight,RoundedCornerShape(17.dp))
    ){
        val boardSide=maxWidth
        val pad=boardSide*.105f
        val step=(boardSide-pad*2)/4
        Canvas(Modifier.fillMaxSize()){
            val edge=size.minDimension
            val margin=edge*.105f
            val gap=(edge-margin*2)/4
            drawRoundRect(Color(0xFF76502C).copy(alpha=.19f),
                topLeft=Offset(8.dp.toPx(),8.dp.toPx()),
                size=androidx.compose.ui.geometry.Size(edge-16.dp.toPx(),edge-16.dp.toPx()),
                cornerRadius=androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
                style=Stroke(2.dp.toPx()))
            for(i in 0..4){
                val p=margin+i*gap
                drawLine(Color(0xFF59391E),Offset(margin,p),Offset(edge-margin,p),
                    strokeWidth=2.1.dp.toPx(),cap=StrokeCap.Round)
                drawLine(Color(0xFF59391E),Offset(p,margin),Offset(p,edge-margin),
                    strokeWidth=2.1.dp.toPx(),cap=StrokeCap.Round)
            }
            for(r in 0..3)for(c in 0..3){
                val x=margin+c*gap;val y=margin+r*gap
                val start=if((r+c)%2==0)Offset(x,y) else Offset(x+gap,y)
                val end=if((r+c)%2==0)Offset(x+gap,y+gap) else Offset(x,y+gap)
                drawLine(Color(0xFF604022),start,end,strokeWidth=1.8.dp.toPx())
            }
            for(i in 0..24){
                val r=i/5;val c=i%5
                drawCircle(Royal.WoodDark,radius=5.dp.toPx(),
                    center=Offset(margin+c*gap,margin+r*gap))
                drawCircle(Royal.GoldLight.copy(alpha=.7f),radius=2.1.dp.toPx(),
                    center=Offset(margin+c*gap,margin+r*gap))
            }
        }
        Text("ॐ",fontSize=(boardSide.value*.21f).sp,color=Royal.WoodDark.copy(alpha=.10f),
            fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.Center))
        val destinations=if(selected!=null)allowed.filter { it.from==selected }.map { it.to }.toSet()
            else emptySet()
        for(i in 0 until 25){
            val p=game.board[i]
            val active=i==selected
            val destination=i in destinations
            val suggestion=hint?.to==i
            val last=game.lastMove?.to==i
            val r=i/5;val c=i%5
            val x=pad+step*c-24.dp
            val y=pad+step*r-24.dp
            val description=if(language=="ne")
                "${r+1} पङ्क्ति, ${c+1} स्तम्भ, ${if(p==null)"खाली" else if(p.side==Side.TIGER)"बाघ" else "बाख्रा"}"
                else "Row ${r+1}, column ${c+1}, ${p?.side?.wire?:"empty"}"
            Box(Modifier.offset(x=x,y=y).size(48.dp)
                .semantics { contentDescription=description }
                .clickable(enabled=enabled){
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNode(i)
                },contentAlignment=Alignment.Center){
                if(active||destination||suggestion||last){
                    Box(Modifier.size(if(active)46.dp else 33.dp).clip(CircleShape)
                        .background(when{
                            destination -> Royal.Mint.copy(alpha=.6f)
                            suggestion -> Royal.GoldLight.copy(alpha=.62f)
                            active -> Royal.Ivory.copy(alpha=.42f)
                            else -> Royal.GoldLight.copy(alpha=.24f)
                        }))
                }
                if(p!=null)PieceToken(p.side,size=43.dp,selected=active)
                else if(destination||suggestion)
                    Box(Modifier.size(11.dp).clip(CircleShape)
                        .background(if(destination)Royal.Navy else Royal.GoldDark))
            }
        }
    }
}
