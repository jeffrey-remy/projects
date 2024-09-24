#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <limits.h>
#include <sys/time.h>
#include <assert.h>
#include <unistd.h>
#include <fcntl.h>
#include <vector>
#include <string>
#include <iostream>
#include <ncurses.h>

#define INITIAL_CARDS_IN_HAND 7

// Prints card with a number if it has one, or a given special string for special cards
#define PRINT_CARD(c, special_print) \
  if (c.special == no_special) {                \
    printw("%d", c.number);			\
  }						\
  else {					\
    printw("%s", special_print.c_str());	\
  }						

// all types of colors for cards
typedef enum color {
		    no_color, // for wild cards that do not have a color
		    red,
		    blue,
		    green,
		    yellow
} color_t;

// all types of special cards
typedef enum special {
		      no_special, // for color cards that are numbered
		      skip,
		      reverse,
		      draw_2,
		      wild,
		      wild_draw_4
} special_t;

struct card {
  // number on card, if applicable. Should be between 0-9.
  int number;
  color_t color;
  special_t special;
};

class player {
public:
  std::vector<card> hand;
  int player_num;
  player() {}
  ~player() {}
  
};

std::vector<player> players;
std::vector<card> deck;
card *top; // card on top of discard pile
int quit = 0;

// House rule settings
int seven_o_rule = 0;
int stacking_rule = 0;
int draw_forever_rule = 0;

// Stack count: if stacking is turned on, then increment this whenever a player stacks another +2/+4
int stack_count = 0;

// Selected color in cases of wild cards, for reference
color_t selected_wild_color;

// Keep track if a special card on top was just played, or has already had its effect applied
int special_played = 0;

// amount of time in microseconds between display status messages
int game_speed = 0;

// If on, let the game keep going until only one player is left
int play_til_one = 0;

// keep track of everyone who finished
std::vector<player> finish_list;
int finisher = 0;

// borrowed from pokemon assignment; initializes ncurses and colors
void io_init_terminal(void) {
  initscr();
  raw();
  noecho();
  curs_set(0);
  keypad(stdscr, TRUE);
  start_color();
  init_pair(COLOR_RED, COLOR_RED, COLOR_BLACK);
  init_pair(COLOR_BLUE, COLOR_CYAN, COLOR_BLACK);
  init_pair(COLOR_GREEN, COLOR_GREEN, COLOR_BLACK);
  init_pair(COLOR_YELLOW, COLOR_YELLOW, COLOR_BLACK);
}

// create deck based on the size of players in the game
void create_deck() {
  unsigned long int i;
  card *c = (card*) malloc(sizeof(card));
  int j;
  int col_num;
  color_t color_add;
  unsigned long int player_magnitude = (players.size()-1)/10 + 1;
  
  // add the following cards ((num_players-1) / 10) + 1 times (add one more deck for every 10 players that are in the game)
  for (i = 0; i < player_magnitude; i++) {
    // Four colors of cards
    for (col_num = 0; col_num < 4; col_num++) {
      switch (col_num) {
      case 0:
	color_add = red;
	break;
      case 1:
	color_add = blue;
	break;
      case 2:
	color_add = green;
	break;
      case 3:
	color_add = yellow;
	break;
      }

      
      
      // Add one 0 card for each color
      c->number = 0;
      c->color = color_add;
      c->special = no_special;
      deck.push_back(*c);

      // Add 2 of all other numbered cards
      for (j = 1; j <= 9; j++) {
	c->number = j;
	c->color = color_add;
	c->special = no_special;
	deck.push_back(*c);
	deck.push_back(*c);
      }

      c->number = -1;
      c->color = color_add;
      c->special = skip;
      deck.push_back(*c);
      deck.push_back(*c);

      c->special = reverse;
      deck.push_back(*c);
      deck.push_back(*c);

      c->special = draw_2;
      deck.push_back(*c);
      deck.push_back(*c);
    }

    // Add wild cards
    c->number = -1;
    c->color = no_color;
    c->special = wild;
    deck.push_back(*c);
    deck.push_back(*c);
    deck.push_back(*c);
    deck.push_back(*c);

    c->number = -1;
    c->color = no_color;
    c->special = wild_draw_4;
    deck.push_back(*c);
    deck.push_back(*c);
    deck.push_back(*c);
    deck.push_back(*c);
  }

  free(c);
}

// Display a card based on its color and number/special value
void display_card(card c) {
  std::string special_print;

  // Check for special cards
  switch (c.special) {
  case no_special:
    special_print = "";
    break;
  case skip:
    special_print = "Skip";
    break;
  case reverse:
    special_print = "Reverse";
    break;
  case draw_2:
    special_print = "Draw 2";
    break;
  case wild:
    special_print = "Wild";
    break;
  case wild_draw_4:
    special_print = "Wild Draw 4";
    break;
  }

  // Print card with corresponding color
  switch (c.color) {
  case no_color:
    PRINT_CARD(c, special_print);
    break;
  case red:
    attron(COLOR_PAIR(COLOR_RED));
    PRINT_CARD(c, special_print);
    attroff(COLOR_PAIR(COLOR_RED));
    break;
  case blue:
    attron(COLOR_PAIR(COLOR_BLUE));
    PRINT_CARD(c, special_print);
    attroff(COLOR_PAIR(COLOR_BLUE));
    break;
  case green:
    attron(COLOR_PAIR(COLOR_GREEN));
    PRINT_CARD(c, special_print);
    attroff(COLOR_PAIR(COLOR_GREEN));
    break;
  case yellow:
    attron(COLOR_PAIR(COLOR_YELLOW));
    PRINT_CARD(c, special_print);
    attroff(COLOR_PAIR(COLOR_YELLOW));
    break;
  }
}

// check if a given card is a valid play, based on the current top card
int valid_move(card selected) {
  //move(2, 0);
  //clrtoeol();
  //mvprintw(2, 0, "Comparison between ");
  //display_card(*top);
  //printw(" and ");
  //display_card(selected);
  //printw(": ");
  // check for stacking case
  if (stacking_rule && (top->special == wild_draw_4 || top->special == draw_2)) {
    // if the top card is in effect
    if (stack_count > 0 && special_played) {
      // player can only play similar draw card
      if (selected.special == top->special) {
	//printw("Valid\n");
	refresh();
	//sleep(1);
	return 1;
      }
      // otherwise, we cannot do anything
      //printw("Invalid\n");
      refresh();
      //sleep(1);
      return 0;
    }
    // otherwise, we can proceed as normal
  }
  else if (!stacking_rule && (top->special == wild_draw_4 || top->special == draw_2)) {
    //printw("Invalid\n");
    refresh();
    //sleep(1);
    return 0;
  }
  if (selected.special == top->special && top->special != no_special) {
    //printw("Valid\n");
    refresh();
    //sleep(1);
    return 1;
  }
  if (selected.special == wild || selected.special == wild_draw_4) {
    //printw("Valid\n");
    refresh();
    //sleep(1);
    return 1;
  }
  if ((top->special == wild || top->special == wild_draw_4) && selected.color == selected_wild_color) {
    //printw("Valid\n");
    refresh();
    //sleep(1);
    return 1;
  }
  if (selected.number == top->number && top->number != -1) {
    //printw("Valid\n");
    refresh();
    //sleep(1);
    return 1;
  }
  if (selected.color == top->color && top->color != no_color) {
    //printw("Valid\n");
    refresh();
    //sleep(1);
    return 1;
  }
  
  
  //printw("Invalid\n");
  refresh();
  //sleep(1);
  // otherwise, this is not a valid card
  return 0;
}

// print list of all players in the game (with card count next to them) and let user select a player 
// there are two modes for the function - displaying, and selecting. The only difference is that selecting mode *requires* that the user selects
// a trainer to terminate display. Displaying can be terminated at any time with ESC, and cannot select a player.
// in the case of selecting mode, the index of the selected player is returned, otherwise -1 is returned.
int display_player_list(int selecting_mode) {
  unsigned long int i;
  unsigned long int select_index = 0;
  int input;
  int selection_over = 0;
  unsigned long int offset = 0;

  move(2, 0);
  clrtoeol();
  if (!selecting_mode) {
    mvprintw(2, 0, "Here are all of the players in the game. (Press ESC to return to cards)");
  }
  else if (seven_o_rule && top->number == 7) {
    mvprintw(2, 0, "Select a player to swap hands with (confirm with space).");
  }
  
  for (i = 4; i < 24; i++) {
    move(i, 0);
    clrtoeol();
  }
  refresh();

  // if there are 20 players or less, we can display them all on the same page
  if (players.size() < 21) {
    move(4, 0);
    for (i = 0; i < players.size(); i++) {
      if (players[i].player_num == 0) {
	printw("You (%d cards)", players[i].hand.size());
      }
      else {
	printw("Bot %d (%d cards)", players[i].player_num, players[i].hand.size());
      }
      printw("\n");
    }  
  }
  refresh();

  while (!selection_over) {
    // display list of players for >20 players
    if (players.size() > 20) {
      for (i = 0; i < 20; i++) {
	move(i+4, 0);
	clrtoeol();
	if (players[i+offset].player_num == 0) {
	  printw("You (%d cards)", players[i+offset].hand.size());
	}
	else {
	  printw("Bot %d (%d cards)", players[i+offset].player_num, players[i+offset].hand.size());
	}
	printw("\n");
      }
    }
    refresh();
      
    move(select_index+4-offset, 0);

    clrtoeol();
    printw(">>");
    if (players[select_index].player_num == 0) {
      printw("You (%d cards)", players[select_index].hand.size());
    }
    else {
      printw("Bot %d (%d cards)", players[select_index].player_num, players[select_index].hand.size());
    }
    
    refresh();
      
    input = getch();
    switch (input) {
      // arrow keys change currently selected card for player
    case KEY_DOWN:
      // check if we need to add offset for displaying players
      if (players.size() > 20) {
	if (offset < (players.size()-20)) {
	  offset++;
	}
      }
      if (select_index < players.size()-1) {
	move(select_index+4, 0);
	clrtoeol();
	if (players[select_index].player_num == 0) {
	  printw("You (%d cards)", players[select_index].hand.size());
	}
	else {
	  printw("Bot %d (%d cards)", players[select_index].player_num, players[select_index].hand.size());
	}	
	select_index++;
      }
      refresh();
      break;
    case KEY_UP:
      // check if we need to reduce offset to go back up in the display
      if (players.size() > 20) {
	if (offset) {
	  offset--;
	}
      }
      if (select_index > 0) {
	move(select_index+4, 0);
	clrtoeol();
	if (players[select_index].player_num == 0) {
	  printw("You (%d cards)", players[select_index].hand.size());
	}
	else {
	  printw("Bot %d (%d cards)", players[select_index].player_num, players[select_index].hand.size());
	}
	select_index--;
      }
      refresh();
      break;
    case ' ':
      // select currently selected player if in selecting mode
      if (selecting_mode && players[select_index].player_num != 0) {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "You selected bot %d.", players[select_index].player_num);
	refresh();
	usleep(game_speed);
	for (i = 4; i < 24; i++) {
	  move(i, 0);
	  clrtoeol();
	}
	refresh();
	selection_over = 1;
	return select_index;
      }
      break;
    case 27:
      // escape and exit trainer display if we are in display mode
      if (!selecting_mode) {
	for (i = 4; i < 24; i++) {
	  move(i, 0);
	  clrtoeol();
	}
	refresh();
	selection_over = 1;
      }
      else {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "You have to select a player to exit!");
      }
      break;
    }
 
  }

  return -1;
}

// give player input to make their turn
void player_turn(player *p) {
  unsigned long int i;
  int has_valid_cards = 0;
  int called_uno = 0;

  move(4, 0);
  
  // check player's hand for valid cards
  for (i = 0; i < p->hand.size(); i++) {
    if (valid_move(p->hand[i])) {
      has_valid_cards = 1;
    }
  }

  if (p->hand.size() < 21) {
    for (i = 0; i < p->hand.size(); i++) {
      display_card(p->hand[i]);
      printw("\n");
    }  
  }
 
  if (has_valid_cards) {
    // printw("\nYou have valid cards!\n");
    refresh();

    unsigned long int select_index = 0;
    int input;
    int turn_over = 0;
    unsigned long int offset = 0;

    while (!turn_over) {
      // display list of player's cards again for >20 cards case
      if (p->hand.size() > 20) {
	for (i = 0; i < 20; i++) {
	  move(i+4, 0);
	  clrtoeol();
	  display_card(p->hand[i+offset]);
	  printw("\n");
	}
      }
      
      move(select_index+4-offset, 0);

      clrtoeol();
      printw(">>");
      display_card(p->hand[select_index]);
      refresh();
      
      input = getch();
      switch (input) {
	// arrow keys change currently selected card for player
      case KEY_DOWN:
	// check if we need to add offset for displaying cards
	if (p->hand.size() > 20) {
	  if (offset < (p->hand.size()-20)) {
	    offset++;
	  }
	}
	if (select_index < p->hand.size()-1) {
	  move(select_index+4, 0);
	  clrtoeol();
	  display_card(p->hand[select_index]);
	  select_index++;
	}
	refresh();
	break;
      case KEY_UP:
	// check if we need to reduce offset to go back up in the display
	if (p->hand.size() > 20) {
	  if (offset) {
	    offset--;
	  }
	}
	if (select_index > 0) {
	  move(select_index+4, 0);
	  clrtoeol();
	  display_card(p->hand[select_index]);
	  select_index--;
	}
	refresh();
	break;
      // play currently selected card
      case ' ':
	// check if move is valid
	if (valid_move(p->hand[select_index])) {
	  card *temp = (card*) malloc(sizeof(card));

	  
	  // add previous top card back into deck
	  deck.push_back(*top);
	  // set new top card
	  // memcpy(top, &temp, sizeof(card));
	  temp->special = p->hand[select_index].special;
	  temp->color = p->hand[select_index].color;
	  temp->number = p->hand[select_index].number;

	  top = temp;

	  // remove card from player's hand
	  p->hand.erase(p->hand.begin() + select_index);

	  

	  // check if player forgot to call Uno and penalize appropriately
	  if (p->hand.size() == 1 && !called_uno) {
	    move(0, 0);
	    clrtoeol();
	    mvprintw(2, 0, "You forgot to call Uno! Draw 2 cards!\n");
	    refresh();
	    usleep(2*game_speed);

	    int draw_index;
	    for (draw_index = 0; draw_index < 2; draw_index++) {
	      int rand_val = rand() % deck.size();

	      p->hand.push_back(deck[rand_val]);

	      move(2, 0);
	      clrtoeol();
	      mvprintw(2, 0, "You drew a ");
	      display_card(deck[rand_val]);
	      printw(".\n");
	      refresh();
	      usleep(game_speed);
	  
	      deck.erase(deck.begin()+rand_val); // remove card from deck	  
	    }
	  }

	  turn_over = 1;

	  special_played = 1;
	}
	else {
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "You can't play that card!\n");
	}
	break;
      case 'Q': // quit the game
	turn_over = 1;
	quit = 1;
	break;
      case 'U': // call Uno
	if (p->hand.size() == 2) {
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "You called Uno!\n");
	  refresh();
	  usleep(game_speed);
	  called_uno = 1;
	}
	break;
      case 'P': // print player list for display, but do not let player select any players
	display_player_list(0);
	break;
      }
      

      refresh();

      
    }
  }
  // force player to draw if they do not have any valid cards
  else {
    mvprintw(2, 0, "You can't play any cards! You have to draw!\n");
    refresh();
    usleep(game_speed);

    // check if player has to keep drawing until they get a valid card, or just draw 1
    if (draw_forever_rule) {
      int draw_valid = 0;

      while (!draw_valid) {
	int rand_val = rand() % deck.size();
	// if the card drawn is valid, play it, otherwise add it to player's hand
	if (valid_move(deck[rand_val])) {
	  card *temp = (card*) malloc(sizeof(card));

	  // add previous top card back into deck
	  deck.push_back(*top);
	  
	  // set new top card
	  temp->special = deck[rand_val].special;
	  temp->color = deck[rand_val].color;
	  temp->number = deck[rand_val].number;

	  top = temp;

	  deck.erase(deck.begin()+rand_val); // remove card from deck	  

	  draw_valid = 1;

	  move(2, 0);
	  clrtoeol();
	  printw("Drew and played a ");
	  display_card(*top);
	  printw(".\n");
	  refresh();

	  special_played = 1;
	}
	else {
	  move(2, 0);
	  clrtoeol();
	  printw("Drew a ");
	  display_card(deck[rand_val]);
	  printw(".\n");
	  refresh();

	  p->hand.push_back(deck[rand_val]);
	  
	  deck.erase(deck.begin()+rand_val); // remove card from deck	    
	}

	
	usleep(game_speed);
      }
    }
    // just draw one card in normal case, and add it to player's hand
    else {
      int rand_val = rand() % deck.size();

      p->hand.push_back(deck[rand_val]);

      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "Drew a ");
      display_card(deck[rand_val]);
      printw(".\n");
      refresh();
      
      deck.erase(deck.begin()+rand_val); // remove card from deck
      

      
    }
    
    usleep(game_speed);
  }
  
}

// play out a bot's turn 
void bot_turn(player *p) {
   unsigned long int i;
   int has_valid_cards = 0;

   move(4, 0);
  
   // check player's hand for valid cards
   for (i = 0; i < p->hand.size(); i++) {
     if (valid_move(p->hand[i])) {
       has_valid_cards = 1;
     }
   }

   // choose bot's card if they have valid cards, otherwise force them to draw from the deck
   if (has_valid_cards) {
     int turn_over = 0;
     
     // current strategy: play first valid card in hand
     i = 0;
     while (i < p->hand.size() && !turn_over) {
       if (valid_move(p->hand[i])) {
	 card *temp = (card*) malloc(sizeof(card));

	 // Uno status message
	 if (p->hand.size() == 2) {
	   move(2, 0);
	   clrtoeol();
	   mvprintw(2, 0, "Bot %d called Uno!", p->player_num);
	   refresh();
	   usleep(game_speed);
	 }

	 // add previous top card back into deck
	 deck.push_back(*top);

	 // set new top card
	 // top = &p->hand[i];
	 // memcpy(top, &temp, sizeof(card));
	 temp->special = p->hand[i].special;
	 temp->color = p->hand[i].color;
	 temp->number = p->hand[i].number;

	 top = temp;
	 
	 // remove card from player's hand
	 p->hand.erase(p->hand.begin() + i);
	 
	 move(2, 0);
	 clrtoeol();
	 mvprintw(2, 0, "Bot %d played ", p->player_num);
	 display_card(*top);
	 printw(".\n");

	 refresh();
	 usleep(game_speed);
	 
	 turn_over = 1;
       }
       i++;
     }

     special_played = 1;
   }
   // force bot to draw cards
   else {
     if (draw_forever_rule) {
       int draw_valid = 0;

       while (!draw_valid) {
	 int rand_val = rand() % deck.size();
	 // if the card drawn is valid, play it, otherwise add it to player's hand
	 if (valid_move(deck[rand_val])) {
	   card *temp = (card*) malloc(sizeof(card));

	   // add previous top card back into deck
	   deck.push_back(*top);

	   // set new top card
	   // top = &deck[rand_val];
	   temp->special = deck[rand_val].special;
	   temp->color = deck[rand_val].color;
	   temp->number = deck[rand_val].number;

	   top = temp;
	   
	   // memcpy(top, &temp, sizeof(card));	   

	   deck.erase(deck.begin()+rand_val); // remove card from deck

	   move(2, 0);
	   clrtoeol();
	   mvprintw(2, 0, "Bot %d drew and played a ", p->player_num);
	   display_card(*top);
	   printw(".\n");
	   refresh();
	   usleep(game_speed);  
	   
	   draw_valid = 1;	   

	   special_played = 1;
	 }
	 else {
	   p->hand.push_back(deck[rand_val]);

	   move(2, 0);
	   clrtoeol();
	   // Uncomment for debugging purposes, but in the real game we will not see other players' cards unless they play them
	   // mvprintw(2, 0, "Bot %d drew a ", p->player_num);
	   // display_card(deck[rand_val]);
	   // printw(".\n");
	   mvprintw(2, 0, "Bot %d is drawing cards...", p->player_num);
	   refresh();
	   usleep(game_speed);
	   
	   deck.erase(deck.begin()+rand_val); // remove card from deck

	   
	   
	 }
       }
     }
     // just draw one
     else {
       int rand_val = rand() % deck.size();
      
       p->hand.push_back(deck[rand_val]);

       // move(2, 0);
       // clrtoeol();
       // Uncomment for debugging purposes, but in the real game we will not see other players' cards unless they play them
       //mvprintw(2, 0, "Bot %d drew a ", p->player_num);
       //display_card(deck[rand_val]);
       //printw(".\n");

       deck.erase(deck.begin()+rand_val); // remove card from deck

       move(2, 0);
       clrtoeol();
       mvprintw(2, 0, "Bot %d drew a card.", p->player_num);
       refresh();
       usleep(game_speed);

       
       
     }
   }
}

// deal hands to all players
void deal_hands() {
  unsigned long int i;
  int j;
  int rand_val;
  
  for (i = 0; i < players.size(); i++) {
    for (j = 0; j < INITIAL_CARDS_IN_HAND; j++) {
      // deal random cards to each player's hand
      rand_val = rand() % deck.size();
      players[i].hand.push_back(deck[rand_val]);
      deck.erase(deck.begin()+rand_val); // remove card from deck
    }
  }
}

// let player select a color for wild cards
void select_wild_color() {
  move(2, 0);
  clrtoeol();
  mvprintw(2, 0, "Pick a color: (r)ed, (b)lue, (y)ellow, (g)reen\n");
  refresh();

  int valid_input = 0;
  while (!valid_input) {
    int input = getch();
    switch (input) {
    case 'r':
      selected_wild_color = red;
      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "You picked red.\n");
      valid_input = 1;
      break;
    case 'b':
      selected_wild_color = blue;
      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "You picked blue.\n");
      valid_input = 1;
      break;
    case 'y':
      selected_wild_color = yellow;
      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "You picked yellow.\n");
      valid_input = 1;
      break;
    case 'g':
      selected_wild_color = green;
      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "You picked green.\n");
      valid_input = 1;
      break;
    }
  }
  refresh();
  usleep(game_speed);
}

// display top card (and the selected color if appropriate)
void display_top_card() {
  move(0, 0);
  clrtoeol();
  mvprintw(0, 0, "Top card: ");
  display_card(*top);
  
  if (top->special == wild || top->special == wild_draw_4) {
    printw(" (Selected Color: ");
    switch (selected_wild_color) {
    case red:
      attron(COLOR_PAIR(COLOR_RED));
      printw("Red");
      attroff(COLOR_PAIR(COLOR_RED));
      break;
    case blue:
      attron(COLOR_PAIR(COLOR_BLUE));
      printw("Blue");
      attroff(COLOR_PAIR(COLOR_BLUE));
      break;
    case green:
      attron(COLOR_PAIR(COLOR_GREEN));
      printw("Green");
      attroff(COLOR_PAIR(COLOR_GREEN));
      break;
    case yellow:
      attron(COLOR_PAIR(COLOR_YELLOW));
      printw("Yellow");
      attroff(COLOR_PAIR(COLOR_YELLOW));
      break;
    default:
      break;
    }
    printw(")");
  }
}


// main game loop
void game_loop() {
  // start order as player-down-list
  int turn_order = 1;
  int game_over = 0;
  int index = 0;
  int rand_val;

  stacking_rule = 1;

  // place random card as first card on the discard pile
  rand_val = rand() % deck.size();
  top = (card*) malloc(sizeof(card));
  top = &deck[rand_val];
  deck.erase(deck.begin() + rand_val);

  clear();
  mvprintw(0, 0, "Top card: ");
  display_card(*top);
  printw("\n");
  printw("--------------------------------------------------------------------------------");
  printw("You are the first player.\n");
  printw("--------------------------------------------------------------------------------\n");
  refresh();
  sleep(1);

  // Check random card for special properties (Wild, Draw 2/4, etc.) before starting game

  move(2, 0);
  clrtoeol();
  // Skip: skip player's turn 
  if (top->special == skip) {
    mvprintw(2, 0, "Oh no! Starting with Skip means you miss a turn.");
    index++;
  }
  // Reverse: reverse the order, and the first player is no longer the user
  else if (top->special == reverse) {
    mvprintw(2, 0, "Oh no! Starting with Reverse means the game starts in reverse without you!");
    turn_order = !turn_order;
    index = players.size()-1; // start with last player instead of first
  }
  // Draw 2: player draws 2 cards and misses their turn
  else if (top->special == draw_2) {
    mvprintw(2, 0, "Oh no! Starting with Draw 2 means you have to draw two cards!");
    refresh();
    sleep(1);

    int draw_index;
    for (draw_index = 0; draw_index < 2; draw_index++) {
      int rand_val = rand() % deck.size();

      players[index].hand.push_back(deck[rand_val]);

      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "You drew a ");
      display_card(deck[rand_val]);
      printw(".\n");
      refresh();
      usleep(game_speed);
	  
      deck.erase(deck.begin()+rand_val); // remove card from deck
    }

    // player misses their turn
    index++;
  }
  // Wild: player gets to choose first color
  else if (top->special == wild) {
    mvprintw(2, 0, "Starting with the Wild card means you get to choose the first color!");
    refresh();
    sleep(1);
    select_wild_color();
  }
  // Wild Draw 4: redraw until we get a non wild draw 4 card (and for our purposes, let's just redraw
  // until we get a non-special card
  else if (top->special == wild_draw_4) {
    int valid_card = 0;
    mvprintw(2, 0, "Whoops! We can't start with Wild Draw 4, let's change that!");

    // add Wild Draw 4 back into deck
    deck.push_back(*top);

    // change top to a non-special card
    card *temp = (card*) malloc(sizeof(card));
    while (!valid_card) {
      rand_val = rand() % deck.size();

      temp->special = deck[rand_val].special;
      temp->color = deck[rand_val].color;
      temp->number = deck[rand_val].number;

      if (temp->special == no_special) {
	top = temp;
	deck.erase(deck.begin() + rand_val);
	valid_card = 1;
      }
       
    }

    move(0, 0);
    clrtoeol();
    mvprintw(0, 0, "Top card: ");
    display_card(*top);
  }
	
  

  refresh();
  if (game_speed / 1000000.0 > 1) {
    usleep(game_speed);
  }
  else {
    sleep(1);
  }
  
  while (!quit && !game_over) {
    clear();
    display_top_card();
    printw("\n");
    printw("--------------------------------------------------------------------------------");
    printw("Status info goes here\n");
    printw("--------------------------------------------------------------------------------\n");
    refresh();
    
    // if this is the player, allow for input
    if (players[index].player_num == 0) {
      player_turn(&players[index]);
    }
    // otherwise, we have a bot turn
    else {
      bot_turn(&players[index]);
    }

    // display new top card
    move(0, 0);
    clrtoeol();
    mvprintw(0, 0, "Top card: ");
    display_card(*top);
    refresh();

    // check current player's hand for win condition
    if (players[index].hand.size() == 0) {
      // check if we keep going afterwards
      if (play_til_one) {
	// add to finisher list
	finish_list.push_back(players[index]);
	finisher++;
	
	if (players[index].player_num == 0) {	  
	  clear();
	  switch (finisher) {
	  case 1:
	    mvprintw(0, 0, "You finished in 1st place! Congratulations!!!\n", finisher);
	    break;
	  case 2:
	    mvprintw(0, 0, "You finished in 2nd place! So close!\n");
	    break;
	  case 3:
	    mvprintw(0, 0, "You finished in 3rd place! Nearly there!\n");
	    break;
	  default:
	    mvprintw(0, 0, "You finished in %dth place! Better luck next time!\n", finisher);
	    break;
	  }
	  
	  refresh();
	  sleep(3);
	}
	else {
	  clear();
	  switch (finisher) {
	  case 1:
	    mvprintw(0, 0, "Bot %d finished in 1st place!\n", players[index].player_num);
	    break;
	  case 2:
	    mvprintw(0, 0, "Bot %d finished in 2nd place!\n", players[index].player_num);
	    break;
	  case 3:
	    mvprintw(0, 0, "Bot %d finished in 3rd place!\n", players[index].player_num);
	    break;
	  default:
	    mvprintw(0, 0, "Bot %d finished in %dth place!\n", players[index].player_num, finisher);
	    break;
	  }
	  
	  refresh();
	  sleep(1);
	}
	
	// remove player from game
	players.erase(players.begin() + index);
      }
      else {
	if (players[index].player_num == 0) {
	  clear();
	  mvprintw(0, 0, "You win! Congrats!\n");
	  refresh();
	  sleep(3);
	}
	else {
	  clear();
	  mvprintw(0, 0, "Bot %d won! You lost!\n", players[index].player_num);
	  refresh();
	  sleep(3);
	}

	game_over = 1;
      }
    }

    // check if there is only one player left
    if (play_til_one && players.size() == 1) {
      clear();
      if (players[0].player_num == 0) {
	mvprintw(0, 0, "You came in dead last!\n");
      }
      else {
	mvprintw(0, 0, "Bot %d came in dead last!\n", players[0].player_num);
      }
      refresh();
      sleep(3);

      game_over = 1;
    }

    // check to reset stack count
    if (top->special != draw_2 && top->special != wild_draw_4) {
      stack_count = 0;
    }

    // check the new top card for special effects (house rules and skips, reverses, etc.)

    // Stacking check
    if (stacking_rule && stack_count && special_played) {
      /*move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "Added onto the stack!", stack_count);
      refresh();
      usleep(game_speed);*/
    }

    // If Seven-O is turned on, check for 7 and swap hands if it was played
    if (seven_o_rule && top->number == 7) {
      if (players[index].player_num == 0) {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "7 played! You get to swap hands!");
	refresh();
	usleep(game_speed);
	
	// show player list and let player select who to swap hands with
	int swap_index = display_player_list(1);
	std::vector<card> temp_hand = players[index].hand;
	players[index].hand = players[swap_index].hand;
	players[swap_index].hand = temp_hand;

	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "You swapped hands with Bot %d!", players[swap_index].player_num);
	refresh();
	usleep(game_speed);
      }
      else {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "7 played! Bot %d gets to swap hands!", players[index].player_num);
	refresh();
	usleep(game_speed);

	int min_cards_index = 0;
	// pick the player with the least cards in their hand
	unsigned long int i;
	for (i = 0; i < players.size(); i++) {
	  if (players[i].hand.size() < players[min_cards_index].hand.size()) {
	    min_cards_index = i;
	  }
	}
	
	std::vector<card> temp_hand = players[index].hand;
	players[index].hand = players[min_cards_index].hand;
	players[min_cards_index].hand = temp_hand;

	move(2, 0);
	clrtoeol();
	if (players[min_cards_index].player_num == 0) {
	  mvprintw(2, 0, "Oh no! Bot %d swapped hands with you!", players[index].player_num);
	}
	else {
	  mvprintw(2, 0, "Bot %d swapped hands with Bot %d!", players[index].player_num, players[min_cards_index].player_num);
	}
	refresh();
	usleep(game_speed);
      }
    }
    // If Seven-O is turned on, check for 0 and rotate hands if it was played
    if (seven_o_rule && top->number == 0) {
      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "0 played! Everyone rotates hands!", players[index].player_num);
      refresh();
      usleep(game_speed);

      // rotate hands in direction of play
      if (turn_order) {
	// if we want to give each hand to the next player in the vector, we can save the last player hand and go in reverse order from the end
	std::vector<card> last_hand = players[players.size()-1].hand;
	int i;
	for (i = players.size()-1; i > 0; i--) {
	  // set current hand to previous player's hand
	  players[i].hand = players[i-1].hand;
	}
	// then, assign last hand to first player
	players[0].hand = last_hand;
      }
      else {	
	// similar to above, but in reverse
	std::vector<card> first_hand = players[0].hand;
	unsigned long int i;
	for (i = 0; i < players.size(); i++) {
	  // set current hand to previous player's hand
	  players[i].hand = players[i+1].hand;
	}
	// then, assign first hand to last player
	players[players.size()-1].hand = first_hand;

      }
      
      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "Everyone's hands were rotated!", players[index].player_num);
      refresh();
      usleep(game_speed);
    }
    
    // Skip: skip over the next player's turn and display a message about it
    if (top->special == skip && special_played) {
      // iterate down
      if (turn_order) {
	index++;
	// loop back to start of vector if we reach the end
	if ((unsigned long int) index == players.size()) {
	  index = 0;
	}
      }
      // iterate up
      else {
	index--;
	if (index == -1) {
	  index = players.size()-1;
	}
      }
      if (players[index].player_num == 0) {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "Your turn was skipped!");
	refresh();
	usleep(game_speed);
      }
      else {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "Bot %d's turn was skipped!", players[index].player_num);
	refresh();
	usleep(game_speed);
      }
      special_played = 0;
    }
    // Reverse: invert turn_order
    if (top->special == reverse && special_played) {
      turn_order = !turn_order;

      move(2, 0);
      clrtoeol();
      mvprintw(2, 0, "The turn order was reversed!");
      refresh();
      usleep(game_speed);
      
      special_played = 0;
    }
    // Draw 2: force next player to draw 2 (also, increase stack count if stacking is on)
    if (top->special == draw_2 && special_played) {
      // iterate to next player
      if (turn_order) {
	index++;
	// loop back to start of vector if we reach the end
	if ((unsigned long int) index == players.size()) {
	  index = 0;
	}
      }
      else {
	index--;
	if (index == -1) {
	  index = players.size()-1;
	}
      }
      if (players[index].player_num == 0) {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "You had to draw 2 cards!");
	refresh();
	usleep(game_speed);

	int draw_index;
	for (draw_index = 0; draw_index < 2; draw_index++) {
	  int rand_val = rand() % deck.size();

	  players[index].hand.push_back(deck[rand_val]);

	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "You drew a ");
	  display_card(deck[rand_val]);
	  printw(".\n");
	  refresh();
	  usleep(game_speed);
	  
	  deck.erase(deck.begin()+rand_val); // remove card from deck	  
	}
      }
      else {	
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "Bot %d had to draw 2 cards!", players[index].player_num);
	refresh();
	usleep(game_speed);

	int draw_index;
	for (draw_index = 0; draw_index < 2; draw_index++) {
	  int rand_val = rand() % deck.size();

	  players[index].hand.push_back(deck[rand_val]);
	  
	  deck.erase(deck.begin()+rand_val); // remove card from deck	  
	}
      }
    
      special_played = 0;
    }
    // Wild card - let player select the color
    if (top->special == wild && special_played) {
      // give player control if appropriate
      if (players[index].player_num == 0) {
	select_wild_color();
      }
      // current bot strategy: pick random color
      else {
	int col_pick = rand() % 4;
	switch (col_pick) {
	case 0:
	  selected_wild_color = red;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked red.\n", players[index].player_num);
	  break;
	case 1:
	  selected_wild_color = blue;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked blue.\n", players[index].player_num);
	  break;
	case 2:
	  selected_wild_color = yellow;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked yellow.\n", players[index].player_num);
	  break;
	case 3:
	  selected_wild_color = green;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked green.\n", players[index].player_num);
	  break;
	}	
      }
      refresh();
      usleep(game_speed);

      stack_count++;
      special_played = 0;
    }
    // Wild Draw Four - select a color, then force next player to draw four
    if (top->special == wild_draw_4 && special_played) {
      // give player control if appropriate
      if (players[index].player_num == 0) {
	select_wild_color();
      }
      else {
	int col_pick = rand() % 4;
	switch (col_pick) {
	case 0:
	  selected_wild_color = red;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked red.\n", players[index].player_num);
	  break;
	case 1:
	  selected_wild_color = blue;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked blue.\n", players[index].player_num);
	  break;
	case 2:
	  selected_wild_color = yellow;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked yellow.\n", players[index].player_num);
	  break;
	case 3:
	  selected_wild_color = green;
	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "Bot %d picked green.\n", players[index].player_num);
	  break;
	}	
      }

      refresh();
      usleep(game_speed);
      display_top_card();

      // then, iterate to next player and apply draw-4 effects
      // iterate to next player
      if (turn_order) {
	index++;
	// loop back to start of vector if we reach the end
	if ((unsigned long int) index == players.size()) {
	  index = 0;
	}
      }
      else {
	index--;
	if (index == -1) {
	  index = players.size()-1;
	}
      }
      if (players[index].player_num == 0) {
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "You had to draw 4 cards!");
	refresh();
	usleep(game_speed);

	int draw_index;
	for (draw_index = 0; draw_index < 4; draw_index++) {
	  int rand_val = rand() % deck.size();

	  players[index].hand.push_back(deck[rand_val]);

	  move(2, 0);
	  clrtoeol();
	  mvprintw(2, 0, "You drew a ");
	  display_card(deck[rand_val]);
	  printw(".\n");
	  refresh();
	  usleep(game_speed);
	  
	  deck.erase(deck.begin()+rand_val); // remove card from deck	  
	}
      }
      else {	
	move(2, 0);
	clrtoeol();
	mvprintw(2, 0, "Bot %d had to draw 4 cards!", players[index].player_num);
	refresh();
	usleep(game_speed);

	int draw_index;
	for (draw_index = 0; draw_index < 4; draw_index++) {
	  int rand_val = rand() % deck.size();

	  players[index].hand.push_back(deck[rand_val]);
	  
	  deck.erase(deck.begin()+rand_val); // remove card from deck	  
	}
      }
      stack_count++;
      special_played = 0;
    }


    // then, move to the next player's turn
    // iterate down
    if (turn_order) {
      index++;
      // loop back to start of vector if we reach the end
      if ((unsigned long int) index == players.size()) {
	index = 0;
      }
    }
    // iterate up
    else {
      index--;
      if (index == -1) {
	index = players.size()-1;
      }
    }
  }

  free(top);
  
}

// initialize game settings
void init_game() {
  int input;
  int valid_input = 0;
  float speed;
  int num_bots;
  int i;

  // check if player wants Seven-O house rules
  clear();
  mvprintw(0, 0, "Do you want Seven-O rules (sevens swap hands, zeroes rotate hands)? (y/n)\n");
  refresh();
  valid_input = 0;
  while (!valid_input) {
    input = getch();
    switch (input) {
    case 'y':
      seven_o_rule = 1;
      valid_input = 1;
      break;
    case 'n':
      seven_o_rule = 0;
      valid_input = 1;
      break;
    }
  }
  
  mvprintw(0, 0, "When you don't have a card to play, do you want to \n(1) draw one card or \n(2) draw until you get a valid card?\n");
  refresh();

  valid_input = 0;
  while (!valid_input) {
    input = getch();
    switch (input) {
    case '1':
      draw_forever_rule = 0;
      valid_input = 1;
      break;
    case '2':
      draw_forever_rule = 1;
      valid_input = 1;
      break;
    }
  }

  clear();
  mvprintw(0, 0, "Set the game speed in milliseconds (Enter a float).\n");
  refresh();

  scanf("%f", &speed);
  game_speed = (speed * 1000);

  clear();
  mvprintw(0, 0, "How many bots do you want in the game?\n");
  refresh();

  // add bots to the game
  scanf("%d", &num_bots);
  for (i = 0; i < num_bots; i++) {
    player *p = new player();
    p->player_num = i+1;
    players.push_back(*p);
    delete p;
  }

  // check if player wants to keep the game going after the first person wins
  clear();
  mvprintw(0, 0, "Do you want the game to keep going until only one player is left? (y/n)\n");
  refresh();

  valid_input = 0;
  while (!valid_input) {
    input = getch();
    switch (input) {
    case 'y':
      play_til_one = 1;
      valid_input = 1;
      break;
    case 'n':
      play_til_one = 0;
      valid_input = 1;
      break;
    }
  }
}

// initialize and run Uno
int main(int argc, char *argv[]) {
  //unsigned long int i;
  
  io_init_terminal();

  srand(time(NULL));

  player *p = new player();
  p->player_num = 0;
  players.push_back(*p);
  
  init_game();  

  create_deck();
  
  deal_hands();
  
  game_loop();

  delete p;
  
  deck.clear();

  /*
  for (i = 0; i < players.size(); i++) {
    delete &players[i];
    }*/
  
  players.clear(); 
  
  endwin();
  
  return 0;
}
