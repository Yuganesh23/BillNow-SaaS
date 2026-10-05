import React, { createContext, useContext, useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from './AuthContext';

const BranchContext = createContext(null);

export const BranchProvider = ({ children }) => {
  const { user } = useAuth();
  const [branches, setBranches] = useState([]);
  const [activeBranchId, setActiveBranchId] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Only fetch branches for Shop Owners
    if (user?.role === 'SHOP_OWNER') {
      fetchBranches();
    }
  }, [user]);

  const fetchBranches = async () => {
    try {
      setLoading(true);
      const res = await axiosClient.get('/shops');
      setBranches(res.data);
      
      const stored = localStorage.getItem('activeBranchId');
      if (stored && res.data.find(b => b.id.toString() === stored)) {
        setActiveBranchId(stored);
      } else if (res.data.length > 0) {
        setActiveBranchId(res.data[0].id.toString());
      }
    } catch (err) {
      console.error("Failed to fetch branches", err);
    } finally {
      setLoading(false);
    }
  };

  const changeBranch = (id) => {
    if (!id) {
      localStorage.removeItem('activeBranchId');
      setActiveBranchId(null);
    } else {
      localStorage.setItem('activeBranchId', id.toString());
      setActiveBranchId(id.toString());
    }
    // Reload page to refresh all data context
    window.location.reload();
  };

  return (
    <BranchContext.Provider value={{ branches, activeBranchId, changeBranch, loading, fetchBranches }}>
      {children}
    </BranchContext.Provider>
  );
};

export const useBranch = () => useContext(BranchContext);
