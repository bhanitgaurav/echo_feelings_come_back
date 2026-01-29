package com.bhanit.apps.echo.features.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bhanit.apps.echo.core.designsystem.components.EchoCard
import com.bhanit.apps.echo.core.ui.ShimmerEffect
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun DashboardPreview() {
    DashboardShimmer()
}


@Composable
fun DashboardShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                ShimmerEffect(
                    modifier = Modifier
                        .width(150.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(8.dp))
                ShimmerEffect(
                    modifier = Modifier
                        .width(200.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            ShimmerEffect(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Activity Graph Card
        EchoCard(elevation = 0.dp) {
             Column(modifier = Modifier.padding(16.dp)) {
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceBetween
                 ) {
                     ShimmerEffect(modifier = Modifier.width(100.dp).height(24.dp).clip(RoundedCornerShape(4.dp)))
                     ShimmerEffect(modifier = Modifier.width(40.dp).height(24.dp).clip(RoundedCornerShape(12.dp)))
                 }
                 Spacer(Modifier.height(16.dp))
                 ShimmerEffect(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(200.dp)
                         .clip(RoundedCornerShape(8.dp))
                 )
             }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             repeat(3) {
                 EchoCard(modifier = Modifier.weight(1f).padding(4.dp)) {
                      Box(modifier = Modifier.height(80.dp).fillMaxWidth().padding(8.dp)) {
                          ShimmerEffect(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)))
                          ShimmerEffect(modifier = Modifier.width(40.dp).height(24.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(4.dp)))
                      }
                 }
             }
        }
    }
}
