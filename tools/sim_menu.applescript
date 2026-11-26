-- Simulator.app menus: "list" prints every menu item; "click <words>" clicks the first item whose
-- name contains one of the words (e.g. "Unfold" or "Fold"). Used for the iPhone Duo, whose fold
-- state can only be changed from the Simulator app.
on run argv
	set action to item 1 of argv
	tell application id "com.apple.iphonesimulator" to activate
	delay 2
	tell application "System Events" to tell process "Simulator"
		set out to ""
		repeat with m in menu bar items of menu bar 1
			try
				repeat with i in menu items of menu 1 of m
					try
						set n to name of i
						if n is not missing value then
							if action is "list" then
								set out to out & (name of m) & " > " & n & linefeed
							else
								repeat with w in (items 2 thru -1 of argv)
									if n contains (w as text) then
										click i
										return "clicked: " & (name of m) & " > " & n
									end if
								end repeat
							end if
						end if
					end try
				end repeat
			end try
		end repeat
		if action is "list" then return out
		return "not found"
	end tell
end run
