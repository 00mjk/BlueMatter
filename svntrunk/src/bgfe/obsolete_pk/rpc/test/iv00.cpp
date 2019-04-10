/* Copyright 2001, 2019 IBM Corporation
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 * following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE 
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 #include <stdio.h>
#include <pk/rpc.hpp>

class MyArgs
  {
  public:
    int x;
    MyArgs(int ax = 7) { x = ax; }
  };

class MyResults
  {
  public:
    int y;
  };

MyResults MyFunction( MyArgs &Args )
  {
  MyResults Rc;
  Rc.y = Args.x + 1;
  return( Rc );
  }

PkMain(int argc, char **argv, char **envp)
  {
  MyResults Res;
  for( int i = 0; i < 10; i++)
    {
    MyArgs Args(i);
    int TargetTask = 0;
#ifdef MPI
    TargetTask = i % MpiSubSys::GetTaskCnt();
#endif
    PkRpc<MyArgs,MyResults>::Execute( TargetTask, MyFunction, Args, Res );
#ifndef PK_BGL_UNI
    printf(" %d ", Res.y );
#endif
    }
    return 0 ;
  }
