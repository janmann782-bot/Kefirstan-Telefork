package com.kefirgram.aurelia;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.FiltersSetupActivity;

import java.util.ArrayList;
import java.util.Locale;

/** Discord-style navigation backed by the account's real Telegram folders. */
public class KefirSpacesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private final ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
    private LinearLayout rail;
    private RecyclerListView list;
    private TextView empty;
    private ChatsAdapter adapter;
    private int selectedFilterId;
    private String query = "";

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        getNotificationCenter().addObserver(this, NotificationCenter.dialogsNeedReload);
        getNotificationCenter().addObserver(this, NotificationCenter.dialogFiltersUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogsNeedReload);
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogFiltersUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setTitle("KefirGram");
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = root;

        ScrollView railScroll = new ScrollView(context);
        railScroll.setVerticalScrollBarEnabled(false);
        railScroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        rail = new LinearLayout(context);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        railScroll.addView(rail);
        root.addView(railScroll, LayoutHelper.createLinear(72, LayoutHelper.MATCH_PARENT));

        LinearLayout chats = new LinearLayout(context);
        chats.setOrientation(LinearLayout.VERTICAL);
        root.addView(chats, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f));
        EditTextBoldCursor search = new EditTextBoldCursor(context);
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        search.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        search.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        search.setHint(LocaleController.getString(R.string.KefirFindChat));
        search.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        search.setText(query);
        chats.addView(search, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
                reloadDialogs();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        empty = new TextView(context);
        empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        empty.setTextSize(15);
        empty.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(24), AndroidUtilities.dp(18), AndroidUtilities.dp(24));
        chats.addView(empty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        list = new RecyclerListView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter = new ChatsAdapter());
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                MessagesController controller = getMessagesController();
                LinearLayoutManager layout = (LinearLayoutManager) list.getLayoutManager();
                if (dy > 0 && selectedFilterId == 0 && query.trim().isEmpty()
                        && !controller.isDialogsEndReached(0) && !controller.isLoadingDialogs(0)
                        && layout.findLastVisibleItemPosition() >= dialogs.size() - 10) {
                    controller.loadDialogs(0, 0, 100, false);
                }
            }
        });
        list.setOnItemClickListener((view, position) -> {
            if (position >= dialogs.size()) return;
            long id = dialogs.get(position).id;
            Bundle args = new Bundle();
            if (DialogObject.isEncryptedDialog(id)) args.putInt("enc_id", DialogObject.getEncryptedChatId(id));
            else if (id > 0) args.putLong("user_id", id);
            else args.putLong("chat_id", -id);
            if (getMessagesController().checkCanOpenChat(args, this)) presentFragment(new ChatActivity(args));
        });
        chats.addView(list, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f));
        rebuildRail();
        reloadDialogs();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rail != null) {
            rebuildRail();
            reloadDialogs();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (rail == null) return;
        if (id == NotificationCenter.dialogFiltersUpdated || id == NotificationCenter.dialogsNeedReload) rebuildRail();
        reloadDialogs();
    }

    private void rebuildRail() {
        rail.removeAllViews();
        MessagesController controller = getMessagesController();
        MessagesController.DialogFilter selected = controller.dialogFiltersById.get(selectedFilterId);
        if (selectedFilterId != 0 && (selected == null || selected.locked)) selectedFilterId = 0;
        addSpace("К", LocaleController.getString(R.string.KefirAllChats), selectedFilterId == 0, () -> selectSpace(0));
        for (MessagesController.DialogFilter filter : controller.dialogFilters) {
            if (filter.isDefault() || filter.locked) continue;
            String name = filter.name == null ? "" : filter.name;
            String label = filter.emoticon != null && !filter.emoticon.isEmpty() ? filter.emoticon
                    : name.isEmpty() ? "#" : name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
            addSpace(label, name, selectedFilterId == filter.id, () -> selectSpace(filter.id));
        }
        addSpace("+", LocaleController.getString(R.string.KefirManageSpaces), false, () -> presentFragment(new FiltersSetupActivity()));
        addSpace("⚙", LocaleController.getString(R.string.KefirAppearance), false, () -> presentFragment(new KefirGramSettingsActivity()));
    }

    private void addSpace(String label, String description, boolean selected, Runnable action) {
        TextView icon = new TextView(getContext());
        icon.setText(label);
        icon.setTextSize(21);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        icon.setContentDescription(description);
        icon.setSelected(selected);
        icon.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(selected ? 16 : 22),
                Theme.getColor(selected ? Theme.key_chat_outBubble : Theme.key_windowBackgroundWhite)));
        if (selected) icon.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        rail.addView(icon, LayoutHelper.createLinear(48, 48, 0, 0, 0, 10));
        icon.setOnClickListener(v -> action.run());
        icon.setOnLongClickListener(v -> {
            android.widget.Toast.makeText(getContext(), description, android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void selectSpace(int id) {
        selectedFilterId = id;
        rebuildRail();
        reloadDialogs();
        list.scrollToPosition(0);
    }

    private void reloadDialogs() {
        if (adapter == null) return;
        MessagesController controller = getMessagesController();
        MessagesController.DialogFilter filter = selectedFilterId == 0 ? null : controller.dialogFiltersById.get(selectedFilterId);
        actionBar.setSubtitle(filter == null ? LocaleController.getString(R.string.KefirAllChats) : filter.name);
        dialogs.clear();
        ArrayList<TLRPC.Dialog> source = filter == null ? controller.getDialogs(0) : filter.dialogs;
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (TLRPC.Dialog dialog : source) {
            if (dialog instanceof TLRPC.TL_dialogFolder) continue;
            if (needle.isEmpty() || title(dialog.id).toLowerCase(Locale.ROOT).contains(needle)) dialogs.add(dialog);
        }
        empty.setVisibility(dialogs.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(LocaleController.getString(needle.isEmpty() ? R.string.KefirSpaceEmpty : R.string.KefirNoChatsFound));
        adapter.notifyDataSetChanged();
    }

    private TLObject peer(long id) {
        if (DialogObject.isEncryptedDialog(id)) {
            TLRPC.EncryptedChat chat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(id));
            return chat == null ? null : getMessagesController().getUser(chat.user_id);
        }
        return id > 0 ? getMessagesController().getUser(id) : getMessagesController().getChat(-id);
    }

    private String title(long id) {
        TLObject object = peer(id);
        if (object instanceof TLRPC.User) return UserObject.getUserName((TLRPC.User) object);
        if (object instanceof TLRPC.Chat) return ((TLRPC.Chat) object).title;
        return LocaleController.getString(R.string.HiddenName);
    }

    private final class ChatsAdapter extends RecyclerListView.SelectionAdapter {
        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }
        @Override public int getItemCount() { return dialogs.size(); }
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new ChatRow(parent.getContext()));
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((ChatRow) holder.itemView).bind(dialogs.get(position));
        }
    }

    private final class ChatRow extends LinearLayout {
        private final BackupImageView avatar;
        private final AvatarDrawable placeholder = new AvatarDrawable();
        private final TextView name;
        private final TextView preview;
        private final TextView unread;

        ChatRow(Context context) {
            super(context);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
            setMinimumHeight(AndroidUtilities.dp(76));
            setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            avatar = new BackupImageView(context);
            avatar.setRoundRadius(AndroidUtilities.dp(22));
            addView(avatar, LayoutHelper.createLinear(44, 44, 0, 0, 12, 0));
            LinearLayout text = new LinearLayout(context);
            text.setOrientation(VERTICAL);
            addView(text, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
            name = new TextView(context);
            name.setTextSize(16);
            name.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            text.addView(name);
            preview = new TextView(context);
            preview.setTextSize(14);
            preview.setMaxLines(2);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            preview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            text.addView(preview);
            unread = new TextView(context);
            unread.setTextSize(12);
            unread.setGravity(Gravity.CENTER);
            unread.setMinWidth(AndroidUtilities.dp(24));
            unread.setTextColor(Theme.getColor(Theme.key_chats_unreadCounterText));
            unread.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_chats_unreadCounter)));
            addView(unread, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 24, 8, 0, 0, 0));
        }

        void bind(TLRPC.Dialog dialog) {
            TLObject object = peer(dialog.id);
            if (object instanceof TLRPC.User) placeholder.setInfo((TLRPC.User) object);
            else if (object instanceof TLRPC.Chat) placeholder.setInfo((TLRPC.Chat) object);
            else placeholder.setInfo(dialog.id, "?", null);
            if (object instanceof TLRPC.User || object instanceof TLRPC.Chat) avatar.getImageReceiver().setForUserOrChat(object, placeholder);
            else avatar.setImageDrawable(placeholder);
            name.setText(title(dialog.id));
            ArrayList<MessageObject> messages = getMessagesController().dialogMessage.get(dialog.id);
            MessageObject last = messages == null || messages.isEmpty() ? null : messages.get(0);
            preview.setText(last == null ? "" : last.messageText);
            int count = getMessagesController().getDialogUnreadCount(dialog);
            unread.setVisibility(count > 0 || dialog.unread_mark ? VISIBLE : GONE);
            unread.setText(count > 99 ? "99+" : count > 0 ? Integer.toString(count) : "•");
        }
    }
}
