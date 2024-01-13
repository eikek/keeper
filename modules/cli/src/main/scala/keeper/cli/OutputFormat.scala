package keeper.cli

enum OutputFormat:
  case Json
  case Text

  def fold[A](json: => A, text: => A): A =
    this match
      case Json => json
      case Text => text
