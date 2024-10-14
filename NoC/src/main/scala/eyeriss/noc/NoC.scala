package eyeriss.noc

import chisel3._
import chisel3.experimental._
import chisel3.util._
import eyeriss.utils._
import eyeriss.Param._

class PlaceHolder extends BlackBox {
    val io = IO(new Bundle{
        val I = Flipped(new ClusterIO)
        val O = new ClusterIO

        val Mode = Input(UInt(2.W))
    })

}

class Cluster(Row: Int, Col: Int) extends Module {
    val I = IO(Flipped(new ClusterIO))
    val O = IO(new ClusterIO)

    val io = IO(new Bundle{
        val Mode = Input(UInt(2.W))
    })

    // Cluster中包含GLB阵列，Router阵列，PE阵列

    val pl = Module(new PlaceHolder)
    pl.io.I <> I
    pl.io.O <> O
    pl.io.Mode := io.Mode
} // Cluster

class NoC extends Module {
    val io = IO(new Bundle{
        val ModeScanChain = Input(Bool())
    })

    // Mesh网络由Cluster组成
    val ClusterArray = for (i <- 0 until NoCHeight) yield {
        for (j <- 0 until NoCWidth) yield Module(new Cluster(i, j))
    }

    // Mesh连接
    for (i <- 0 until NoCHeight; j <- 0 until NoCWidth) {
        if (i == 0) 
            ClusterArray(i)(j).I.N <> 0.U.asTypeOf(new ClusterPort('N'))
        else 
            ClusterArray(i)(j).I.N <> ClusterArray(i-1)(j).O.S

        if (i == NoCHeight-1)
            ClusterArray(i)(j).I.S <> 0.U.asTypeOf(new ClusterPort('S'))
        else 
            ClusterArray(i)(j).I.S <> ClusterArray(i+1)(j).O.N

        if (j == 0)
            ClusterArray(i)(j).I.W <> 0.U.asTypeOf(new ClusterPort('W'))
        else 
            ClusterArray(i)(j).I.W <> ClusterArray(i)(j-1).O.E

        if (j == NoCWidth-1)
            ClusterArray(i)(j).I.E <> 0.U.asTypeOf(new ClusterPort('E'))
        else 
            ClusterArray(i)(j).I.E <> ClusterArray(i)(j+1).O.W

    }

    val ModeScanChain = ScanChain(io.ModeScanChain, true.B, NoCWidth*NoCHeight*2)

    for (i <- 0 until NoCHeight; j <- 0 until NoCWidth) {
        ClusterArray(i)(j).io.Mode := ModeScanChain((i*NoCWidth+j+1)*2-1, (i*NoCWidth+j)*2)
    } 
} // NoC

