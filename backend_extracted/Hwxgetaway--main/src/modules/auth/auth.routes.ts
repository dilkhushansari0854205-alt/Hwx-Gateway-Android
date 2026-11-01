import { Router, Request, Response } from 'express';
import { firebaseAuth } from '../../middleware/firebaseAuth.ts';
import { UserService } from '../users/user.service.ts';

const router = Router();

/**
 * GET /v1/auth/me
 * Retrieves current authenticated user profile and account statistics.
 */
router.get('/me', firebaseAuth(), async (req: Request, res: Response): Promise<void> => {
  const uid = req.user!.uid;
  const user = await UserService.getUserById(uid);

  if (!user) {
    res.status(404).json({
      success: false,
      error: { code: 'USER_NOT_FOUND', message: 'User profile not found' },
    });
    return;
  }

  res.status(200).json({
    success: true,
    data: user,
  });
});

export const authRoutes = router;
