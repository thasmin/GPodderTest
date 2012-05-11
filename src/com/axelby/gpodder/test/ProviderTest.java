package com.axelby.gpodder.test;


import android.content.ContentValues;
import android.database.Cursor;
import android.test.ProviderTestCase2;

import com.axelby.gpodder.Client;
import com.axelby.gpodder.Client.Changes;
import com.axelby.gpodder.Provider;

public class ProviderTest extends ProviderTestCase2<Provider> {
	Provider _provider;
	private String _username = "podaxtest";
	private String _password = "podaxtest";

	public ProviderTest() {
		super(Provider.class, Provider.AUTHORITY);
	}
	public ProviderTest(Class<Provider> providerClass, String providerAuthority) {
		super(providerClass, providerAuthority);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		_provider = (Provider) getMockContentResolver()
				.acquireContentProviderClient(Provider.AUTHORITY)
				.getLocalContentProvider();
	}

	private ContentValues makeUrlValues(String url) {
		ContentValues values = new ContentValues();
		values.put("url", url);
		return values;
	}

	public void testInsert() {
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url1"));
		Cursor c = getMockContentResolver().query(Provider.URI, new String[] { "url" }, null, null, null);
		assertTrue(c.moveToNext());
		assertEquals(c.getString(0), "url1");
		c.close();
	}

	public void testDelete() {
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url1"));
		int deleted = getMockContentResolver().delete(Provider.URI, "url = ?", new String[] { "url1" });
		assertEquals(1, deleted);
		Cursor c = getMockContentResolver().query(Provider.URI, new String[] { "url" }, null, null, null);
		assertTrue(c.isAfterLast());
		c.close();
	}

	public void testDeleteAll() {
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url1"));
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url2"));
		int deleted = getMockContentResolver().delete(Provider.URI, null, null);
		assertEquals(2, deleted);
		Cursor c = getMockContentResolver().query(Provider.URI, new String[] { "url" }, null, null, null);
		assertTrue(c.isAfterLast());
		c.close();
	}

	public void testFakeSync() {
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url1"));
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url2"));
		_provider.fakeSync();

		Cursor c = getMockContentResolver().query(Provider.URI, new String[] { "url" }, null, null, null);
		assertTrue(c.moveToNext());
		assertEquals("url1", c.getString(0));
		assertTrue(c.moveToNext());
		assertEquals("url2", c.getString(0));
		assertFalse(c.moveToNext());
		c.close();
	}

	public void testAddAfterSync() {
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url1"));
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url2"));
		_provider.fakeSync();
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url3"));

		Cursor c = getMockContentResolver().query(Provider.URI, new String[] { "url" }, null, null, null);
		assertTrue(c.moveToNext());
		assertEquals("url1", c.getString(0));
		assertTrue(c.moveToNext());
		assertEquals("url2", c.getString(0));
		assertTrue(c.moveToNext());
		assertEquals("url3", c.getString(0));
		assertFalse(c.moveToNext());
		c.close();
	}

	public void testDeleteAfterSync() {
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url1"));
		getMockContentResolver().insert(Provider.URI, makeUrlValues("url2"));
		_provider.fakeSync();
		getMockContentResolver().delete(Provider.URI, "url = ?", new String[] { "url1" });

		Cursor c = getMockContentResolver().query(Provider.URI, new String[] { "url" }, null, null, null);
		assertTrue(c.moveToNext());
		assertEquals("url2", c.getString(0));
		assertFalse(c.moveToNext());
		c.close();
	}

	public void testLogin() {
		Client client = new Client(getMockContext(), _username, _password);
		assertTrue(client.authenticate());
	}

	public void testServer0Clear() {
		Client client = new Client(getMockContext(), _username, _password);
		Changes changes = client.getSubscriptionChanges(0);
		if (changes.added.size() != 0) {
			for(String url : changes.added)
				getMockContentResolver().delete(Provider.URI, "url = ?", new String[] { url });
			client.syncDiffs();
			changes = client.getSubscriptionChanges(0);
		}
		assertEquals(0, changes.added.size());
	}

	public void testServerAddDelete() {
		Client client = new Client(getMockContext(), _username, _password);
		Changes changes = client.getSubscriptionChanges(0);
		assertEquals(0, changes.added.size());

		getMockContentResolver().insert(Provider.URI, makeUrlValues("http://blog.axelby.com/podcast.xml"));
		client.syncDiffs();
		changes = client.getSubscriptionChanges(0);
		assertEquals(1, changes.added.size());
		int timestamp = changes.timestamp;

		getMockContentResolver().delete(Provider.URI, "url = ?", new String[] { "http://blog.axelby.com/podcast.xml" });
		client.syncDiffs();
		changes = client.getSubscriptionChanges(timestamp);
		assertEquals(0, changes.added.size());
		assertEquals(1, changes.removed.size());

		changes = client.getSubscriptionChanges(0);
		assertEquals(0, changes.added.size());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
}
