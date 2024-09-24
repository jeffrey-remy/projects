CS327 Assignment 2 
by Jeffrey Remy

This project implements a singleplayer version of Uno that can be played with an arbitrary number of bots. Game settings include house rules like
swapping and rotating hands, as well as drawing cards for as long as necessary. 

GAME INITIALIZATION 
Starting the program will take you to a series of settings that you can set. These include: 
-HOUSE RULES
--Seven-O: If on, 7s will let players swap hands, and 0s rotate ALL hands.
--Drawing: Normally, you only draw one card if you don't have any valid cards. 
           The other option is to draw until you get a valid card, and then play that card.
-GAME SPEED: Set the speed (in ms) of sleep times between status messages being displayed. (Recommended: ~100-1000)
-BOTS: Set an arbitrary number of bots to play in the game. 
-PLAY-TIL-ONE: If on, the game will keep going until only one player has not discarded their hand. 
               Ideal for larger games (>100). 

HOW TO PLAY
Up key: Go up in a list 
Down key: Go down in a list
Space: Select a card/player
'U': When you have 2 cards, call Uno to avoid penalty (!)
'P': See list of players with card counts
'Q': Quit the game
