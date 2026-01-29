We are building a tic-tac-toe game for android devices. The game should allow two players to play against each other on the same device. The game board should be a 3x3 grid, and players should take turns placing their marks (X or O) in empty cells. The game should detect when a player has won or when the game ends in a draw, and it should display an appropriate message. Additionally, there should be a reset button to start a new game. The user interface should be simple and intuitive, suitable for touch input on mobile devices.

Build the app using Kotlin and Android Studio, following best practices for Android development. Ensure that the app is responsive and works well on different screen sizes. Include comments in the code to explain the logic and flow of the game.

There should be a high score table, comprised of the number of wins for each player. The high score table should persist even when the app is closed and reopened. The app should track the highest 5 scores for each player.

When the app is launched, it should display the main menu with options to start a new game, view the high score table, and exit the app. The high score table should be accessible from the main menu and should display the top 5 scores for each player in descending order.

Ensure that the app handles edge cases, such as invalid input or unexpected behavior, gracefully. Test the app thoroughly to ensure that it functions correctly and provides a smooth user experience.

Build beautiful and simple UI layouts using XML for the game board, main menu, and high score table. Use appropriate colors, fonts, and styles to enhance the visual appeal of the app while maintaining usability.

There should be options to select the difficulty level of the game (easy (dumb!), medium, hard (AI will not forgive, if possible!)) which will affect the AI opponent's skill level when playing against the computer. The AI should make reasonable moves based on the selected difficulty.

```yaml---
description: Instructions for building a tic-tac-toe game for Android devices.
applyTo: **/*.kt, **/*.xml
---

```md