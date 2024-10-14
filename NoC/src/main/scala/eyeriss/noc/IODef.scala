package eyeriss.noc

import chisel3._
import chisel3.experimental._
import chisel3.util._

class IActRouterPort extends Bundle {
    // all Output
    val pl = Output(Bool())
}

class WeightRouterPort extends Bundle {
    // all Output
    val pl = Output(Bool())
}

class PsumRouterPort extends Bundle {
    // all Output
    val pl = Output(Bool())
}

class ClusterPort(Dir: Char) extends Bundle {
    val IAct    = Vec(3, new IActRouterPort)
    val Weight  = if (Dir == 'W' | Dir == 'E') Some(Vec(3, new WeightRouterPort)) else None
    val Psum    = if (Dir == 'N' | Dir == 'S') Some(Vec(3, new PsumRouterPort)) else None

}

class ClusterIO extends Bundle {
    val N = new ClusterPort('N')
    val S = new ClusterPort('S')
    val W = new ClusterPort('W')
    val E = new ClusterPort('E')

}


