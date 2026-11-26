package com.csm.mushafi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.csm.mushafi.data.BookmarkEntity;
import com.csm.mushafi.data.QuranRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** المحفوظات: فتح علامة، تعديل اسمها، أو حذفها. */
public class BookmarksActivity extends BaseActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private QuranRepository repo;
    private RecyclerView list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        repo = new QuranRepository(getApplicationContext());
        list = findViewById(R.id.bookmark_list);
        empty = findViewById(R.id.bookmark_empty);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void reload() {
        io.execute(() -> {
            List<BookmarkEntity> items = repo.dao().allBookmarks();
            main.post(() -> {
                if (isFinishing()) {
                    return;
                }
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                list.setAdapter(new BookmarkAdapter(items, new BookmarkAdapter.Listener() {
                    @Override
                    public void onOpen(BookmarkEntity b) {
                        Intent intent = new Intent(BookmarksActivity.this, PageReaderActivity.class);
                        intent.putExtra(ReaderActivity.EXTRA_SURAH, b.surah);
                        intent.putExtra(ReaderActivity.EXTRA_AYAH, b.ayah);
                        startActivity(intent);
                    }

                    @Override
                    public void onLongPress(BookmarkEntity b) {
                        showEditDialog(b);
                    }
                }));
            });
        });
    }

    private void showEditDialog(BookmarkEntity b) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(b.label);
        input.setSelection(b.label.length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.bookmark_edit_title)
                .setView(input)
                .setPositiveButton(R.string.btn_save, (d, w) -> {
                    String label = input.getText().toString().trim();
                    io.execute(() -> {
                        repo.dao().renameBookmark(b.id, label);
                        reload();
                    });
                })
                .setNeutralButton(R.string.btn_delete, (d, w) -> io.execute(() -> {
                    repo.dao().deleteBookmark(b.id);
                    reload();
                }))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
