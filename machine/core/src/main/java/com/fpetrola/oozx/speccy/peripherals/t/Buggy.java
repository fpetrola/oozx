/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.oozx.speccy.peripherals.t;

import javax.swing.*;
import java.awt.*;

public class Buggy
{
  public static void main( final String[] args )
  {
    final JFrame frame = new JFrame();
    final JLabel labelA = new JLabel( "<html>HTML Emoji character 🦁</html>" );
    final JLabel labelB = new JLabel( "<html>HTML Emoji escape sequence &#129409;</html>" );
    final JLabel labelC = new JLabel( "Emoji character 🦁" );
    //To show there's enough space inside the label and show where each label starts and ends.
    labelA.setOpaque( true );
    labelA.setBackground( Color.YELLOW );
    labelB.setOpaque( true );
    labelB.setBackground( Color.GREEN );
    labelC.setOpaque( true );
    labelC.setBackground( Color.RED );
    frame.setLayout( new GridLayout( 3, 1 ) );
    frame.add( labelA );
    frame.add( labelB );
    frame.add( labelC );
    frame.setSize( 400, 300 );
    frame.setLocationRelativeTo( null );
    frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
    frame.setVisible( true );
  }
}