echo "Deleting the opencode sessions ..."
opencode session list --format json | jq -r '.[].id' | while read -r id; do opencode session delete $id; done
echo "Done."

echo "Check again the list of the session ..."
opencode session list

echo "Finished."