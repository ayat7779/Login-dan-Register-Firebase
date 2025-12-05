const admin = require('firebase-admin');

/**
 * Cloud Function untuk mencegah email duplikat di Auth
 */
exports.preventDuplicateEmailAuth = functions.auth.user().onCreate(async (user) => {
  const { uid, email } = user;

  if (!email) {
    console.log('User created without email, skipping...');
    return null;
  }

  console.log(`Checking duplicate for email: ${email}, UID: ${uid}`);

  try {
    // Cari semua users dengan email yang sama
    const listUsersResult = await admin.auth().listUsers();
    const duplicateUsers = listUsersResult.users.filter(
      existingUser => existingUser.email === email && existingUser.uid !== uid
    );

    if (duplicateUsers.length > 0) {
      console.log(`❌ DUPLICATE FOUND: Email ${email} already exists!`);
      console.log(`Existing users:`, duplicateUsers.map(u => u.uid));

      // Delete the NEW user (keep the old one)
      await admin.auth().deleteUser(uid);
      console.log(`Deleted duplicate user: ${uid}`);

      // Log the incident
      await admin.database().ref(`logs/duplicate_emails/${Date.now()}`).set({
        email: email,
        duplicateUid: uid,
        existingUids: duplicateUsers.map(u => u.uid),
        timestamp: new Date().toISOString(),
        action: 'deleted_new_user'
      });

      throw new Error(`Email ${email} already registered. Please use another email.`);
    }

    console.log(`✅ Email ${email} is unique, user ${uid} created successfully`);
    return null;

  } catch (error) {
    console.error(`Error preventing duplicate:`, error);
    throw error;
  }
});

/**
 * Cloud Function untuk sync Auth dengan Database dan cek duplikat
 */
exports.syncAuthToDatabase = functions.auth.user().onCreate(async (user) => {
  const { uid, email } = user;

  if (!email) return null;

  const db = admin.database();

  // Cek di database apakah email sudah ada
  const emailQuery = await db.ref('users')
    .orderByChild('email')
    .equalTo(email)
    .once('value');

  if (emailQuery.exists()) {
    console.log(`⚠️ Email ${email} already exists in database, deleting from Auth...`);

    // Delete dari Auth karena sudah ada di database
    await admin.auth().deleteUser(uid);

    await db.ref(`logs/auth_sync_errors/${Date.now()}`).set({
      error: 'email_already_in_database',
      email: email,
      uid: uid,
      timestamp: new Date().toISOString()
    });

    return null;
  }

  // Buat/update user di database
  const userData = {
    uid: uid,
    email: email,
    username: user.displayName || email.split('@')[0],
    phoneNumber: user.phoneNumber || '',
    emailVerified: user.emailVerified || false,
    phoneVerified: false,
    role: 'user',
    status: 1,
    createdAt: admin.database.ServerValue.TIMESTAMP,
    updatedAt: admin.database.ServerValue.TIMESTAMP
  };

  await db.ref(`users/${uid}`).set(userData);
  console.log(`✅ User ${uid} synced to database`);

  return null;
});